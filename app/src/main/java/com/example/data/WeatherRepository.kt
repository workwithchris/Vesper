package com.example.data

import android.net.Uri
import com.example.model.WeatherEffectType
import com.example.model.WeatherInfo
import com.example.model.WeatherLocation
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class WeatherRepository {

  suspend fun fetchWeather(location: WeatherLocation): WeatherInfo? =
    withContext(Dispatchers.IO) {
      val url =
        "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}" +
          "&current=temperature_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,wind_speed_10m" +
          "&timezone=auto"
      val body = fetchJsonLimited(url) ?: return@withContext null
      parseOpenMeteoJson(body, locationName = location.name)
    }

  suspend fun searchCities(query: String): List<WeatherLocation> =
    withContext(Dispatchers.IO) {
      val trimmed = query.trim()
      if (trimmed.length < 2) return@withContext emptyList()
      val encoded = Uri.encode(trimmed)
      val url =
        "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=en&format=json"
      val body = fetchJsonLimited(url) ?: return@withContext emptyList()
      parseGeocodingJson(body)
    }

  private fun fetchJsonLimited(urlString: String): String? {
    var connection: HttpURLConnection? = null
    return try {
      val url = URL(urlString)
      connection =
        (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = 6_000
          readTimeout = 6_000
          setRequestProperty("Accept", "application/json")
        }
      if (connection.responseCode !in 200..299) {
        return null
      }
      connection.inputStream.bufferedReader().use { reader ->
        val buffer = CharArray(8192)
        val sb = StringBuilder()
        var read = reader.read(buffer)
        while (read != -1 && sb.length < 65_536) {
          sb.append(buffer, 0, read)
          read = reader.read(buffer)
        }
        sb.toString()
      }
    } catch (_: Exception) {
      null
    } finally {
      connection?.disconnect()
    }
  }

  companion object {
    val PRESET_CITIES: List<WeatherLocation> =
      listOf(
        WeatherLocation.MIAMI_DEFAULT,
        WeatherLocation("New York", "New York, US", 40.7128, -74.0060),
        WeatherLocation("San Francisco", "California, US", 37.7749, -122.4194),
        WeatherLocation("London", "United Kingdom", 51.5072, -0.1276),
        WeatherLocation("Tokyo", "Japan", 35.6762, 139.6503),
        WeatherLocation("Sydney", "Australia", -33.8688, 151.2093),
      )

    fun parseOpenMeteoJson(jsonString: String, locationName: String = "Miami"): WeatherInfo? {
      return try {
        val root = JSONObject(jsonString)
        val current = root.optJSONObject("current") ?: return null
        if (!current.has("temperature_2m") || !current.has("weather_code")) return null

        val tempC = current.getDouble("temperature_2m").roundToInt()
        val apparentC = current.optDouble("apparent_temperature", tempC.toDouble()).roundToInt()
        val isDay = current.optInt("is_day", 1) == 1
        val precipitation = current.optDouble("precipitation", 0.0)
        val weatherCode = current.getInt("weather_code")
        val cloudCover = current.optInt("cloud_cover", 0).coerceIn(0, 100)
        val windSpeed = current.optDouble("wind_speed_10m", 12.0).roundToInt().coerceAtLeast(0)

        val (effectType, label) =
          mapWmoWeather(
            weatherCode = weatherCode,
            temperatureCelsius = tempC,
            apparentTemperatureCelsius = apparentC,
            cloudCoverPercent = cloudCover,
            precipitationMm = precipitation,
          )

        WeatherInfo(
          temperatureCelsius = tempC,
          apparentTemperatureCelsius = apparentC,
          conditionLabel = label,
          locationName = locationName,
          effectType = effectType,
          cloudCoverPercent = cloudCover,
          windSpeedKmh = windSpeed,
          isDay = isDay,
          isLive = true,
          isRefreshing = false,
        )
      } catch (_: Exception) {
        null
      }
    }

    fun parseGeocodingJson(jsonString: String): List<WeatherLocation> {
      return try {
        val root = JSONObject(jsonString)
        val results = root.optJSONArray("results") ?: return emptyList()
        buildList {
          for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val name = item.optString("name", "").trim()
            if (name.isEmpty() || !item.has("latitude") || !item.has("longitude")) continue
            val admin1 = item.optString("admin1", "").trim()
            val country = item.optString("country", "").trim()
            val region = listOf(admin1, country).filter { it.isNotEmpty() }.joinToString(", ")
            add(
              WeatherLocation(
                name = name,
                region = region,
                latitude = item.getDouble("latitude"),
                longitude = item.getDouble("longitude"),
              )
            )
          }
        }
      } catch (_: Exception) {
        emptyList()
      }
    }

    fun mapWmoWeather(
      weatherCode: Int,
      temperatureCelsius: Int,
      apparentTemperatureCelsius: Int,
      cloudCoverPercent: Int,
      precipitationMm: Double = 0.0,
    ): Pair<WeatherEffectType, String> {
      return when {
        weatherCode in 95..99 -> WeatherEffectType.THUNDERSTORM to "Tropical storm"
        weatherCode in 80..82 -> WeatherEffectType.RAIN to "Tropical showers"
        weatherCode in 61..67 || weatherCode in 71..77 -> WeatherEffectType.RAIN to "Coastal rain"
        weatherCode in 51..57 || precipitationMm > 0.1 -> WeatherEffectType.RAIN to "Light drizzle"
        weatherCode == 45 || weatherCode == 48 -> WeatherEffectType.CLOUDY to "Coastal mist"
        weatherCode == 3 -> WeatherEffectType.CLOUDY to "Overcast"
        weatherCode == 2 -> {
          if (cloudCoverPercent < 55 && (temperatureCelsius >= 29 || apparentTemperatureCelsius >= 30)) {
            WeatherEffectType.HEAT_HAZE to "Warm & breezy"
          } else {
            WeatherEffectType.CLOUDY to "Partly cloudy"
          }
        }
        temperatureCelsius >= 32 || apparentTemperatureCelsius >= 33 ->
          WeatherEffectType.HEAT_HAZE to "Tropical heat"
        temperatureCelsius >= 29 || apparentTemperatureCelsius >= 30 ->
          WeatherEffectType.HEAT_HAZE to "Warm sun"
        weatherCode == 0 -> WeatherEffectType.CLEAR_BREEZE to "Clear sky"
        else -> WeatherEffectType.CLEAR_BREEZE to "Mostly clear"
      }
    }
  }
}
