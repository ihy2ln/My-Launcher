package com.homelauncher.app.ui.search

import kotlin.math.pow
import kotlin.math.round

sealed class MicroResult {
    data class Calculation(val expression: String, val result: String) : MicroResult()
    data class UnitConversion(val from: String, val to: String, val result: String) : MicroResult()
}

object SearchEngine {
    private val calcRegex = Regex("""^[\d\s+\-*/().^%]+$""")
    private val unitRegex = Regex(
        """^\s*([\d.]+)\s*(km|m|cm|mm|mi|ft|in|kg|g|lb|oz|c|f|k|l|ml|gal)\s*(?:to|in|->)\s*(km|m|cm|mm|mi|ft|in|kg|g|lb|oz|c|f|k|l|ml|gal)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    fun microResults(query: String): List<MicroResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<MicroResult>()

        unitRegex.matchEntire(trimmed)?.let { match ->
            val value = match.groupValues[1].toDoubleOrNull() ?: return@let
            val from = match.groupValues[2].lowercase()
            val to = match.groupValues[3].lowercase()
            convertUnit(value, from, to)?.let { converted ->
                results += MicroResult.UnitConversion(
                    from = "$value $from",
                    to = to,
                    result = formatNumber(converted),
                )
            }
        }

        if (calcRegex.matches(trimmed) && trimmed.any { it in "+-*/^%" }) {
            evaluate(trimmed)?.let { value ->
                results += MicroResult.Calculation(trimmed, formatNumber(value))
            }
        }

        return results
    }

    private fun convertUnit(value: Double, from: String, to: String): Double? {
        fun toMeter(v: Double, u: String) = when (u) {
            "km" -> v * 1000
            "m" -> v
            "cm" -> v / 100
            "mm" -> v / 1000
            "mi" -> v * 1609.344
            "ft" -> v * 0.3048
            "in" -> v * 0.0254
            else -> null
        }
        fun fromMeter(v: Double, u: String) = when (u) {
            "km" -> v / 1000
            "m" -> v
            "cm" -> v * 100
            "mm" -> v * 1000
            "mi" -> v / 1609.344
            "ft" -> v / 0.3048
            "in" -> v / 0.0254
            else -> null
        }
        fun toGram(v: Double, u: String) = when (u) {
            "kg" -> v * 1000
            "g" -> v
            "lb" -> v * 453.59237
            "oz" -> v * 28.349523125
            else -> null
        }
        fun fromGram(v: Double, u: String) = when (u) {
            "kg" -> v / 1000
            "g" -> v
            "lb" -> v / 453.59237
            "oz" -> v / 28.349523125
            else -> null
        }
        fun toLiter(v: Double, u: String) = when (u) {
            "l" -> v
            "ml" -> v / 1000
            "gal" -> v * 3.785411784
            else -> null
        }
        fun fromLiter(v: Double, u: String) = when (u) {
            "l" -> v
            "ml" -> v * 1000
            "gal" -> v / 3.785411784
            else -> null
        }

        if (from in listOf("c", "f", "k") && to in listOf("c", "f", "k")) {
            val celsius = when (from) {
                "c" -> value
                "f" -> (value - 32) * 5 / 9
                "k" -> value - 273.15
                else -> return null
            }
            return when (to) {
                "c" -> celsius
                "f" -> celsius * 9 / 5 + 32
                "k" -> celsius + 273.15
                else -> null
            }
        }

        toMeter(value, from)?.let { meters -> return fromMeter(meters, to) }
        toGram(value, from)?.let { grams -> return fromGram(grams, to) }
        toLiter(value, from)?.let { liters -> return fromLiter(liters, to) }
        return null
    }

    private fun evaluate(expression: String): Double? {
        return try {
            val tokens = tokenize(expression.replace(" ", ""))
            val rpn = toRpn(tokens)
            evalRpn(rpn)
        } catch (_: Exception) {
            null
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens += expr.substring(start, i)
                }
                c in "+-*/^%()" -> {
                    if (c == '-' && (tokens.isEmpty() || tokens.last() in "+-*/^(")) {
                        val start = i
                        i++
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                        tokens += expr.substring(start, i)
                    } else {
                        tokens += c.toString()
                        i++
                    }
                }
                else -> throw IllegalArgumentException("bad char")
            }
        }
        return tokens
    }

    private fun precedence(op: String) = when (op) {
        "+", "-" -> 1
        "*", "/", "%" -> 2
        "^" -> 3
        else -> 0
    }

    private fun toRpn(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val stack = ArrayDeque<String>()
        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> output += token
                token == "(" -> stack.addLast(token)
                token == ")" -> {
                    while (stack.isNotEmpty() && stack.last() != "(") output += stack.removeLast()
                    if (stack.isEmpty() || stack.removeLast() != "(") throw IllegalArgumentException("parens")
                }
                else -> {
                    while (stack.isNotEmpty() && precedence(stack.last()) >= precedence(token)) {
                        output += stack.removeLast()
                    }
                    stack.addLast(token)
                }
            }
        }
        while (stack.isNotEmpty()) output += stack.removeLast()
        return output
    }

    private fun evalRpn(tokens: List<String>): Double {
        val stack = ArrayDeque<Double>()
        for (token in tokens) {
            val number = token.toDoubleOrNull()
            if (number != null) {
                stack.addLast(number)
            } else {
                val b = stack.removeLast()
                val a = stack.removeLast()
                stack.addLast(
                    when (token) {
                        "+" -> a + b
                        "-" -> a - b
                        "*" -> a * b
                        "/" -> a / b
                        "%" -> a % b
                        "^" -> a.pow(b)
                        else -> throw IllegalArgumentException("op")
                    },
                )
            }
        }
        return stack.single()
    }

    private fun formatNumber(value: Double): String {
        val rounded = round(value * 1_000_000.0) / 1_000_000.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    }
}
