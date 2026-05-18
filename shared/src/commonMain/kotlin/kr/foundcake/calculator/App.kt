package kr.foundcake.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kr.foundcake.calculator.calculator.CalculatorAction
import kr.foundcake.calculator.calculator.CalculatorReducer

@Composable
@Preview
fun App() {
    MaterialTheme {
        var state by remember { mutableStateOf(CalculatorReducer.initialState()) }
        val keyboard = listOf(
            listOf("C", "⌫", "=", "+"),
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "(", ")")
        )

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.display,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.weight(0.75f)) {
                keyboard.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        row.forEach { token ->
                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp),
                                onClick = {
                                    state = CalculatorReducer.reduce(
                                        state,
                                        when (token) {
                                            "C" -> CalculatorAction.Clear
                                            "⌫" -> CalculatorAction.Backspace
                                            "=" -> CalculatorAction.Evaluate
                                            else -> CalculatorAction.Input(token)
                                        }
                                    )
                                }
                            ) {
                                Text(token)
                            }
                        }
                    }
                }
            }
        }
    }
}
