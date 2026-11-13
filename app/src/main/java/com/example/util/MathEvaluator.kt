package com.example.util

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs

object MathEvaluator {

  private val formatter = DecimalFormat("#,##0.######")
  private val percentOfRegex =
    Regex("""^\s*([+-]?\d+(?:\.\d+)?)\s*%\s*(?:of|\*)\s*([+-]?\d+(?:\.\d+)?)\s*$""", RegexOption.IGNORE_CASE)
  private val unitConvertRegex =
    Regex("""^\s*([+-]?\d+(?:\.\d+)?)\s*([a-zA-Z°]+)\s+(?:to|in)\s+([a-zA-Z°]+)\s*$""", RegexOption.IGNORE_CASE)

  fun evaluateToString(rawExpression: String): String? {
    val trimmed = rawExpression.trim()
    if (trimmed.isEmpty()) return null

    percentOfRegex.matchEntire(trimmed)?.let { match ->
      val pct = match.groupValues[1].toDoubleOrNull() ?: return null
      val base = match.groupValues[2].toDoubleOrNull() ?: return null
      return formatNumber((pct / 100.0) * base)
    }

    unitConvertRegex.matchEntire(trimmed)?.let { match ->
      val amount = match.groupValues[1].toDoubleOrNull() ?: return null
      val fromUnit = normalizeUnit(match.groupValues[2])
      val toUnit = normalizeUnit(match.groupValues[3])
      val converted = convertUnits(amount, fromUnit, toUnit) ?: return null
      return "${formatNumber(converted)} $toUnit"
    }

    val cleaned = trimmed.replace(" ", "").replace("×", "*").replace("÷", "/")
    if (cleaned.isEmpty()) return null
    val hasOperator = cleaned.any { it == '+' || it == '-' || it == '*' || it == '/' || it == '%' }
    if (!hasOperator) return null
    // Single leading negative number like "-5" isn't a calculation
    if (cleaned.startsWith("-") && cleaned.drop(1).none { it in "+-*/%" }) return null

    val value = parseExpression(cleaned) ?: return null
    return formatNumber(value)
  }

  private fun formatNumber(value: Double): String? {
    if (value.isNaN() || value.isInfinite()) return null
    val rounded = if (abs(value - value.toLong()) < 1e-9) value.toLong().toDouble() else value
    return formatter.format(rounded)
  }

  private fun normalizeUnit(raw: String): String {
    return when (raw.lowercase(Locale.US).removePrefix("°")) {
      "km", "kilometer", "kilometers", "kilometre", "kilometres" -> "km"
      "mi", "mile", "miles" -> "mi"
      "m", "meter", "meters", "metre", "metres" -> "m"
      "ft", "foot", "feet" -> "ft"
      "cm", "centimeter", "centimeters" -> "cm"
      "in", "inch", "inches" -> "in"
      "kg", "kilogram", "kilograms" -> "kg"
      "lb", "lbs", "pound", "pounds" -> "lb"
      "g", "gram", "grams" -> "g"
      "oz", "ounce", "ounces" -> "oz"
      "c", "celsius" -> "°C"
      "f", "fahrenheit" -> "°F"
      "l", "liter", "liters", "litre", "litres" -> "L"
      "gal", "gallon", "gallons" -> "gal"
      "gb" -> "GB"
      "mb" -> "MB"
      else -> raw.lowercase(Locale.US)
    }
  }

  private fun convertUnits(value: Double, from: String, to: String): Double? {
    if (from == to) return value
    return when (from to to) {
      "km" to "mi" -> value * 0.62137119
      "mi" to "km" -> value / 0.62137119
      "m" to "ft" -> value * 3.2808399
      "ft" to "m" -> value / 3.2808399
      "cm" to "in" -> value / 2.54
      "in" to "cm" -> value * 2.54
      "kg" to "lb" -> value * 2.20462262
      "lb" to "kg" -> value / 2.20462262
      "g" to "oz" -> value / 28.3495231
      "oz" to "g" -> value * 28.3495231
      "°C" to "°F" -> (value * 9.0 / 5.0) + 32.0
      "°F" to "°C" -> (value - 32.0) * 5.0 / 9.0
      "L" to "gal" -> value * 0.26417205
      "gal" to "L" -> value / 0.26417205
      "GB" to "MB" -> value * 1024.0
      "MB" to "GB" -> value / 1024.0
      else -> null
    }
  }

  private fun parseExpression(expr: String): Double? {
    var pos = 0

    fun peek(): Char? = if (pos < expr.length) expr[pos] else null

    fun parseNumber(): Double? {
      val start = pos
      if (peek() == '+' || peek() == '-') pos++
      var hasDigit = false
      var hasDot = false
      while (pos < expr.length) {
        val ch = expr[pos]
        if (ch.isDigit()) {
          hasDigit = true
          pos++
        } else if (ch == '.' && !hasDot) {
          hasDot = true
          pos++
        } else {
          break
        }
      }
      if (!hasDigit) return null
      return expr.substring(start, pos).toDoubleOrNull()
    }

    var parseTermRef: (() -> Double?)? = null
    var parseAddSubRef: (() -> Double?)? = null

    fun parseFactor(): Double? {
      return when (peek()) {
        '(' -> {
          pos++
          val inner = parseAddSubRef?.invoke() ?: return null
          if (peek() != ')') return null
          pos++
          inner
        }
        '+', '-' -> {
          val sign = if (peek() == '-') -1.0 else 1.0
          pos++
          val inner = parseFactor() ?: return null
          sign * inner
        }
        else -> parseNumber()
      }
    }

    fun parseTerm(): Double? {
      var left = parseFactor() ?: return null
      while (true) {
        val op = peek()
        if (op == '*' || op == '/' || op == '%') {
          pos++
          val right = parseFactor() ?: return null
          left =
            when (op) {
              '*' -> left * right
              '/' -> {
                if (abs(right) < 1e-12) return null
                left / right
              }
              else -> {
                if (abs(right) < 1e-12) return null
                left % right
              }
            }
        } else {
          break
        }
      }
      return left
    }

    fun parseAddSub(): Double? {
      var left = parseTerm() ?: return null
      while (true) {
        val op = peek()
        if (op == '+' || op == '-') {
          pos++
          val right = parseTerm() ?: return null
          left = if (op == '+') left + right else left - right
        } else {
          break
        }
      }
      return left
    }

    parseTermRef = ::parseTerm
    parseAddSubRef = ::parseAddSub

    val result = parseAddSub() ?: return null
    return if (pos == expr.length) result else null
  }
}
