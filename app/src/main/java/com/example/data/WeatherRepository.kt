package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.model.WeatherEffectType
import com.example.model.WeatherInfo
import com.example.model.WeatherLocation
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

sealed interface DeviceLocationResult {
  data class Success(val location: WeatherLocation) : DeviceLocationResult

  data object PermissionRequired : DeviceLocationResult

  data class Unavailable(val reason: String) : DeviceLocationResult
}

class WeatherRepository(private val context: Context? = null) {

  fun hasLocationPermission(): Boolean {
    val appContext = context ?: return false
    val coarseGranted =
      ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val fineGranted =
      ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return coarseGranted || fineGranted
  }

  @SuppressLint("MissingPermission")
  suspend fun detectDeviceLocation(): DeviceLocationResult =
    withContext(Dispatchers.IO) {
      val appContext =
        context ?: return@withContext DeviceLocationResult.Unavailable("Location context unavailable")
      if (!hasLocationPermission()) {
        return@withContext DeviceLocationResult.PermissionRequired
      }

      val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
          ?: return@withContext DeviceLocationResult.Unavailable("Location service unavailable")

      val cachedBest = findBestLastKnownLocation(locationManager)
      val thirtyMinutesMs = 30L * 60L * 1000L
      val isCachedFresh =
        cachedBest != null && (System.currentTimeMillis() - cachedBest.time) <= thirtyMinutesMs

      val resolvedCoords =
        if (isCachedFresh) {
          cachedBest
        } else {
          requestCurrentCoordinates(locationManager) ?: cachedBest
        }
          ?: return@withContext DeviceLocationResult.Unavailable(
            "Turn on device location services to detect your city"
          )

      val resolvedLocation =
        reverseGeocodeLocation(
          context = appContext,
          latitude = resolvedCoords.latitude,
          longitude = resolvedCoords.longitude,
        )
      DeviceLocationResult.Success(resolvedLocation)
    }

  @SuppressLint("MissingPermission")
  private fun findBestLastKnownLocation(locationManager: LocationManager): Location? {
    val candidateProviders =
      buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          add(LocationManager.FUSED_PROVIDER)
        }
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.PASSIVE_PROVIDER)
      }
    var best: Location? = null
    for (provider in candidateProviders) {
      val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: continue
      if (best == null || loc.time > best.time) {
        best = loc
      }
    }
    return best
  }

  @SuppressLint("MissingPermission")
  private suspend fun requestCurrentCoordinates(locationManager: LocationManager): Location? {
    val activeProviders =
      buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
          runCatching { locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER) }.getOrDefault(false)
        ) {
          add(LocationManager.FUSED_PROVIDER)
        }
        if (runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
          add(LocationManager.NETWORK_PROVIDER)
        }
        if (runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
          add(LocationManager.GPS_PROVIDER)
        }
      }
    if (activeProviders.isEmpty()) return null

    val provider = activeProviders.first()
    return withTimeoutOrNull(6_000L) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        withContext(Dispatchers.Main) {
          suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            runCatching {
              locationManager.getCurrentLocation(
                provider,
                signal,
                ContextCompat.getMainExecutor(context!!),
              ) { loc ->
                if (cont.isActive) cont.resume(loc)
              }
            }.onFailure {
              if (cont.isActive) cont.resume(null)
            }
          }
        }
      } else {
        withContext(Dispatchers.Main) {
          suspendCancellableCoroutine { cont ->
            val listener =
              object : LocationListener {
                override fun onLocationChanged(location: Location) {
                  runCatching { locationManager.removeUpdates(this) }
                  if (cont.isActive) cont.resume(location)
                }

                @Deprecated("Deprecated in API 29")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {}
              }
            cont.invokeOnCancellation { runCatching { locationManager.removeUpdates(listener) } }
            @Suppress("DEPRECATION")
            runCatching {
              locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }.onFailure {
              if (cont.isActive) cont.resume(null)
            }
          }
        }
      }
    }
  }

  private suspend fun reverseGeocodeLocation(
    context: Context,
    latitude: Double,
    longitude: Double,
  ): WeatherLocation {
    val androidAddress = lookupAndroidGeocoderAddress(context, latitude, longitude)
    if (androidAddress != null) {
      val city =
        androidAddress.locality?.trim()?.takeIf { it.isNotEmpty() }
          ?: androidAddress.subAdminArea?.trim()?.takeIf { it.isNotEmpty() }
          ?: androidAddress.adminArea?.trim()?.takeIf { it.isNotEmpty() }
      if (city != null) {
        val region =
          listOfNotNull(
            androidAddress.adminArea?.trim()?.takeIf { it.isNotEmpty() && !it.equals(city, ignoreCase = true) },
            androidAddress.countryName?.trim()?.takeIf { it.isNotEmpty() },
          )
            .joinToString(", ")
        return WeatherLocation(
          name = city,
          region = region,
          latitude = latitude,
          longitude = longitude,
        )
      }
    }

    val remoteUrl =
      "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$latitude&longitude=$longitude&localityLanguage=en"
    val remoteJson = fetchJsonLimited(remoteUrl)
    if (remoteJson != null) {
      parseReverseGeocodeJson(remoteJson, latitude, longitude)?.let { return it }
    }

    return WeatherLocation(
      name = "Current Location",
      region = String.format(Locale.US, "%.2f°, %.2f°", latitude, longitude),
      latitude = latitude,
      longitude = longitude,
    )
  }

  private suspend fun lookupAndroidGeocoderAddress(
    context: Context,
    latitude: Double,
    longitude: Double,
  ): Address? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.getDefault())
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      withTimeoutOrNull(4_000L) {
        suspendCancellableCoroutine { cont ->
          runCatching {
            geocoder.getFromLocation(
              latitude,
              longitude,
              1,
              object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                  if (cont.isActive) cont.resume(addresses.firstOrNull())
                }

                override fun onError(errorMessage: String?) {
                  if (cont.isActive) cont.resume(null)
                }
              },
            )
          }.onFailure {
            if (cont.isActive) cont.resume(null)
          }
        }
      }
    } else {
      @Suppress("DEPRECATION")
      runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
    }
  }

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

    fun parseReverseGeocodeJson(
      jsonString: String,
      latitude: Double,
      longitude: Double,
    ): WeatherLocation? {
      return try {
        val root = JSONObject(jsonString)
        val city =
          listOf(
            root.optString("city", "").trim(),
            root.optString("locality", "").trim(),
            root.optString("principalSubdivision", "").trim(),
          )
            .firstOrNull { it.isNotEmpty() }
            ?: return null
        val subdivision = root.optString("principalSubdivision", "").trim()
        val country = root.optString("countryName", "").trim()
        val region =
          listOfNotNull(
            subdivision.takeIf { it.isNotEmpty() && !it.equals(city, ignoreCase = true) },
            country.takeIf { it.isNotEmpty() },
          )
            .joinToString(", ")
        WeatherLocation(
          name = city,
          region = region,
          latitude = latitude,
          longitude = longitude,
        )
      } catch (_: Exception) {
        null
      }
    }

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
