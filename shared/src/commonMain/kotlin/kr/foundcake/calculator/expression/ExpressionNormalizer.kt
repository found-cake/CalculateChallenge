package kr.foundcake.calculator.expression

object ExpressionNormalizer {
    private val allowedChars = "0123456789+-*/(). \t\n\r".toSet()

    private val sanitize: (String) -> String = { raw ->
        raw.filter { char -> char in allowedChars && !char.isWhitespace() }
    }

    fun canonicalize(raw: String): String? {
        val cleaned = sanitize(raw)
        if (cleaned.isEmpty()) return null
        val tokens = tokenize(cleaned) ?: return null
        if (tokens.isEmpty()) return null
        return tokens.joinToString(separator = "")
    }

    fun compact(raw: String): String? {
        val cleaned = sanitize(raw)
        if (cleaned.isEmpty()) return null
        val tokens = tokenize(cleaned, normalizeNumbers = false) ?: return null
        if (tokens.isEmpty()) return null
        return tokens.joinToString(separator = "")
    }

    private fun tokenize(input: String, normalizeNumbers: Boolean = true): List<String>? {
        val tokens = mutableListOf<String>()
        val buffer = StringBuilder()

        fun flushNumber(): Boolean {
            if (buffer.isEmpty()) {
                return true
            } else {
                val number = if (normalizeNumbers) {
                    canonicalizeNumber(buffer.toString()) ?: return false
                } else {
                    validateNumber(buffer.toString()) ?: return false
                }
                tokens += number
                buffer.clear()
                return true
            }
        }

        for (char in input) {
            when {
                char.isDigit() || char == '.' -> buffer.append(char)
                char in "+-*/()" -> {
                    if (!flushNumber()) return null
                    tokens += char.toString()
                }
                else -> return null
            }
        }

        if (!flushNumber()) return null
        return tokens
    }

    private fun validateNumber(token: String): String? {
        if (token.isEmpty()) return null
        if (token == ".") return null
        val dotCount = token.count { it == '.' }
        if (dotCount > 1) return null
        return token
    }

    private fun canonicalizeNumber(token: String): String? {
        validateNumber(token) ?: return null

        val parts = token.split('.')
        val integer = parts[0]
        val fraction = parts.getOrNull(1)

        val normalizedInt = integer.trimStart('0').ifEmpty { "0" }
        val normalizedFraction = fraction?.trimEnd('0')

        return if (normalizedFraction.isNullOrEmpty()) {
            normalizedInt
        } else {
            "$normalizedInt.$normalizedFraction"
        }
    }
}
