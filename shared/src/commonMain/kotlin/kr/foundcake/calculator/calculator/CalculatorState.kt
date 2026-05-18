package kr.foundcake.calculator.calculator

import kr.foundcake.calculator.challenge.HiddenExpressionMatcher
import kr.foundcake.calculator.expression.ExpressionEvaluator
import kr.foundcake.calculator.expression.ExpressionNormalizer

data class CalculatorState(
    val expression: String,
    val display: String,
)

sealed interface CalculatorAction {
    data class Input(val token: String) : CalculatorAction
    data object Clear : CalculatorAction
    data object Backspace : CalculatorAction
    data object Evaluate : CalculatorAction
}

object CalculatorReducer {
    private val allowedInputTokens = setOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "+", "-", "*", "/", "(", ")", "."
    )

    fun initialState(): CalculatorState = CalculatorState(
        expression = "",
        display = "0",
    )

    fun reduce(state: CalculatorState, action: CalculatorAction): CalculatorState = when (action) {
        is CalculatorAction.Input -> onInput(state, action.token)
        CalculatorAction.Clear -> initialState()
        CalculatorAction.Backspace -> onBackspace(state)
        CalculatorAction.Evaluate -> onEvaluate(state)
    }

    private fun onInput(state: CalculatorState, token: String): CalculatorState {
        if (token !in allowedInputTokens) return state
        val nextExpression = state.expression + token
        return state.copy(expression = nextExpression, display = nextExpression)
    }

    private fun onBackspace(state: CalculatorState): CalculatorState {
        val nextExpression = if (state.expression.isEmpty()) "" else state.expression.dropLast(1)
        return state.copy(
            expression = nextExpression,
            display = nextExpression.ifEmpty { "0" }
        )
    }

    private fun onEvaluate(state: CalculatorState): CalculatorState {
        val compact = ExpressionNormalizer.compact(state.expression) ?: return state.copy(display = "ERR")
        val canonical = ExpressionNormalizer.canonicalize(state.expression) ?: return state.copy(display = "ERR")
        val computed = ExpressionEvaluator.evaluate(canonical) ?: return state.copy(display = "ERR")
        val result = HiddenExpressionMatcher.revealIfMatch(compact) ?: computed
        return state.copy(expression = result, display = result)
    }
}
