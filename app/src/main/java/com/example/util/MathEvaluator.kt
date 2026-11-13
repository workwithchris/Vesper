package com.example.util

import java.text.DecimalFormat
import kotlin.math.abs

object MathEvaluator {

  private val formatter = DecimalFormat("#,##0.######")

  fun evaluateToString(rawExpression: String): String? {
    val cleaned = rawExpression.replace(" ", "").replace("×", "*").replace("÷", "/")
    if (cleaned.isEmpty()) return null
    val hasOperator = cleaned.any { it == '+' || it == '-' || it == '*' || it == '/' || it == '%' }
    if (!hasOperator) return null
    // Single leading negative number like "-5" isn't a calculation
    if (cleaned.startsWith("-") && cleaned.drop(1).none { it in "+-*/%" }) return null

    val value = parseExpression(cleaned) ?: return null
    if (value.isNaN() || value.isInfinite()) return null
    val rounded = if (abs(value - value.toLong()) < 1e-9) value.toLong().toDouble() else value
    return formatter.format(rounded)
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
