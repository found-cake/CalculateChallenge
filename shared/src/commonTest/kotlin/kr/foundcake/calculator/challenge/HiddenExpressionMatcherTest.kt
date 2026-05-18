package kr.foundcake.calculator.challenge

import kr.foundcake.calculator.expression.ExpressionNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class HiddenExpressionMatcherTest {
    @Test
    fun equivalentCanonicalExpressionDoesNotRevealFlag() {
        val exact = HiddenExpressionMatcher.exactTarget
        val alternate = insertLeadingZeroIntoFirstNumber(exact)

        assertNotEquals(exact, alternate)
        assertEquals(
            ExpressionNormalizer.canonicalize(exact),
            ExpressionNormalizer.canonicalize(alternate),
        )
        assertFalse(HiddenExpressionMatcher.matches(alternate))
        assertNull(HiddenExpressionMatcher.revealIfMatch(alternate))
    }

    private fun insertLeadingZeroIntoFirstNumber(expression: String): String {
        val index = expression.indexOfFirstIndexed { currentIndex, char ->
            (char.isDigit() || char == '.') &&
                (currentIndex == 0 || !expression[currentIndex - 1].isDigitOrDecimalPoint())
        }
        check(index >= 0) { "Challenge trigger has no numeric token" }
        return expression.substring(0, index) + "0" + expression.substring(index)
    }

    private fun String.indexOfFirstIndexed(predicate: (Int, Char) -> Boolean): Int {
        forEachIndexed { index, char ->
            if (predicate(index, char)) return index
        }
        return -1
    }

    private fun Char.isDigitOrDecimalPoint(): Boolean = isDigit() || this == '.'
}
