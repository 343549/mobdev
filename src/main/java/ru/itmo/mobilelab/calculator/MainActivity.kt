package ru.itmo.mobilelab.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Главная Activity: создаёт ViewModel, в [onCreate] подключает UI через [setContent].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFEAF3FF)
                ) {
                    CalculatorScreen(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Весь экран калькулятора: текст с [CalculatorViewModel.display];
 * каждая кнопка вызывает [CalculatorViewModel.onButtonPressed].
 */
@Composable
private fun CalculatorScreen(viewModel: CalculatorViewModel) {
    // Изменение StateFlow вызывает recomposition этого участка
    val displayText by viewModel.display.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DisplayArea(text = displayText)

        // Один обработчик для всех рядов: метка кнопки уходит во ViewModel
        val onPress: (String) -> Unit = { key -> viewModel.onButtonPressed(key) }

        KeypadRow(labels = listOf("C", "⌫", "%", "÷"), onKey = onPress)
        KeypadRow(labels = listOf("7", "8", "9", "*"), onKey = onPress)
        KeypadRow(labels = listOf("4", "5", "6", "-"), onKey = onPress)
        KeypadRow(labels = listOf("1", "2", "3", "+"), onKey = onPress)
        KeypadRow(labels = listOf("0", ".", "="), onKey = onPress, wideLabel = "0")
    }
}

/**
 * Область отображения числа/выражения. Должна быть в [ColumnScope], чтобы [Modifier.weight]
 * занимал оставшееся место по вертикали.
 */
@Composable
private fun ColumnScope.DisplayArea(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f) // Занимает оставшееся место в Column; ряды кнопок остаются внизу
            .background(
                color = Color(0xFFD6E8FF),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            fontSize = 40.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0A2E66),
            maxLines = 1
        )
    }
}

/**
 * Ряд кнопок; если задан [wideLabel] (например «0»), эта кнопка в два раза шире.
 */
@Composable
private fun KeypadRow(
    labels: List<String>,
    onKey: (String) -> Unit,
    wideLabel: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (label in labels) {
            // В Row weight делит ширину между соседними кнопками
            val cellWeight = if (label == wideLabel) 2f else 1f
            CalculatorButton(
                label = label,
                modifier = Modifier
                    .weight(cellWeight)
                    .height(70.dp),
                onClick = { onKey(label) }
            )
        }
    }
}

/**
 * Одна кнопка калькулятора; у операторов другой цвет, чем у цифр.
 */
@Composable
private fun CalculatorButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isOperator = label in listOf("÷", "*", "-", "+", "=")
    val buttonColor = if (isOperator) Color(0xFF1565C0) else Color(0xFF90CAF9)
    val textColor = if (isOperator) Color.White else Color(0xFF0A2E66)

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
