package kr.foundcake.calculator.challenge

import kr.foundcake.calculator.expression.ExpressionNormalizer

object HiddenExpressionMatcher {
    private val secretBuilder = GeneratedChallengeMaterial.exactTargetCodes.map { code ->
        { code.toChar().toString() }
    }

    internal val exactTarget: String = ExpressionNormalizer.compact(
        secretBuilder.joinToString(separator = "") { part -> part() }
    ) ?: error("Hidden exact expression missing")

    fun matches(compact: String): Boolean = compact == exactTarget

    fun revealIfMatch(compact: String): String? = if (matches(compact)) {
        FlagVault.reveal(compact)
    } else {
        null
    }
}
