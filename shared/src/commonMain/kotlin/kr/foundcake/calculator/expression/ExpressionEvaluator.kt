package kr.foundcake.calculator.expression

import kotlin.math.abs

object ExpressionEvaluator {
    private val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2)

    fun evaluate(canonical: String): String? {
        val tokens = toTokens(canonical)
        val rpn = toRpn(tokens) ?: return null
        val value = evalRpn(rpn) ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return render(value)
    }

    private fun toTokens(canonical: String): List<String> {
        val out = mutableListOf<String>()
        val number = StringBuilder()

        canonical.forEach { c ->
            when {
                c.isDigit() || c == '.' -> number.append(c)
                c in "+-*/()" -> {
                    if (number.isNotEmpty()) {
                        out += number.toString()
                        number.clear()
                    }
                    out += c.toString()
                }
            }
        }
        if (number.isNotEmpty()) out += number.toString()
        return out
    }

    private fun toRpn(tokens: List<String>): List<String>? {
        val output = mutableListOf<String>()
        val ops = ArrayDeque<String>()
        var previous: String? = null

        tokens.forEach { token ->
            when {
                token.firstOrNull()?.isDigit() == true -> {
                    output += token
                    previous = token
                }
                token in precedence -> {
                    val unary = token == "-" && (previous == null || previous in precedence || previous == "(")
                    if (unary) output += "0"
                    while (ops.isNotEmpty() && ops.last() in precedence &&
                        precedence.getValue(ops.last()) >= precedence.getValue(token)
                    ) {
                        output += ops.removeLast()
                    }
                    ops += token
                    previous = token
                }
                token == "(" -> {
                    ops += token
                    previous = token
                }
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.last() != "(") {
                        output += ops.removeLast()
                    }
                    if (ops.isEmpty() || ops.removeLast() != "(") return null
                    previous = token
                }
                else -> return null
            }
        }

        while (ops.isNotEmpty()) {
            val op = ops.removeLast()
            if (op == "(") return null
            output += op
        }
        return output
    }

    private fun evalRpn(tokens: List<String>): Double? {
        val values = ArrayDeque<Double>()
        tokens.forEach { token ->
            when {
                token in precedence -> {
                    if (values.size < 2) return null
                    val rhs = values.removeLast()
                    val lhs = values.removeLast()
                    val result = when (token) {
                        "+" -> lhs + rhs
                        "-" -> lhs - rhs
                        "*" -> lhs * rhs
                        "/" -> if (rhs == 0.0) return null else lhs / rhs
                        else -> return null
                    }
                    values += result
                }
                else -> values += token.toDoubleOrNull() ?: return null
            }
        }
        return if (values.size == 1) values.last() else null
    }

    private fun render(value: Double): String {
        val rounded = value.toLong().toDouble()
        if (abs(value - rounded) < 1e-10) return rounded.toLong().toString()
        return value.toString()
    }
}
