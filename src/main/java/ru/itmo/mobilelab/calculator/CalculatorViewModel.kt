package ru.itmo.mobilelab.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Логика калькулятора: хранит текст на дисплее, обновляет при нажатии клавиш,
 * по «=» вычисляет выражение. Состояние сохраняется в [SavedStateHandle],
 * чтобы не терялось при повороте экрана.
 */
class CalculatorViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_DISPLAY = "display"
        private const val DEFAULT_DISPLAY = "0"
    }

    private val _display = MutableStateFlow(savedStateHandle[KEY_DISPLAY] ?: DEFAULT_DISPLAY)
    val display: StateFlow<String> = _display.asStateFlow()

    /** Меняет текст на дисплее и записывает его в saved state. */
    private fun setDisplay(value: String) {
        _display.value = value
        savedStateHandle[KEY_DISPLAY] = value
    }

    /**
     * Точка входа из UI: метка нажатой кнопки (цифра, оператор, C, ⌫, %, = и т.д.).
     * В зависимости от типа кнопки вызывается нужная функция.
     */
    fun onButtonPressed(label: String) {
        when (label) {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> appendDigit(label)
            "+", "-", "*", "÷" -> appendOperator(label)
            "." -> appendDot()
            "C" -> setDisplay(DEFAULT_DISPLAY)
            "⌫" -> backspace()
            "%" -> applyPercent()
            "=" -> calculate()
        }
    }

    /**
     * Добавляет цифру в конец строки (или заменяет начальный «0»).
     * Если на экране ошибка, ввод цифры начинает ввод заново.
     */
    private fun appendDigit(digit: String) {
        val current = _display.value
        if (screenShowsError(current)) {
            // После ошибки ввод цифры сбрасывает экран и начинается с этой цифры
            if (digit.all { it.isDigit() }) setDisplay(digit)
            return
        }
        if (current == DEFAULT_DISPLAY && digit.all { it.isDigit() }) {
            // Вместо «05» показываем только «5»
            setDisplay(digit)
        } else {
            setDisplay(current + digit)
        }
    }

    /**
     * Добавляет оператор (+ - * ÷) к строке; итоговое вычисление пока не выполняется.
     * Если последний символ уже оператор, заменяет его новым (например + после *).
     */
    private fun appendOperator(op: String) {
        val current = _display.value
        if (current.isEmpty()) {
            // Только «-» в пустой строке — как знак отрицательного числа
            if (op == "-") setDisplay(op)
            return
        }
        if (lastCharIsOperator(current)) {
            // Два оператора подряд: заменить последний на новый
            setDisplay(current.dropLast(1) + op)
            return
        }
        if (screenShowsError(current)) return
        setDisplay(current + op)
    }

    /**
     * Добавляет десятичную точку только к «последнему» числу; в каждом числе не больше одной точки.
     */
    private fun appendDot() {
        val current = _display.value
        if (current.isEmpty() || screenShowsError(current)) return
        // После разбиения по операторам последний фрагмент — текущее вводимое число
        val pieces = current.split(Regex("[+\\-*÷]"))
        val lastNumber = pieces.lastOrNull() ?: return
        if (!lastNumber.contains('.')) {
            setDisplay(current + ".")
        }
    }

    /**
     * Читает строку с дисплея как выражение: сначала приоритет × и ÷, потом + и −;
     * форматирует результат на экране. Ошибки из try/catch показываются текстом.
     */
    private fun calculate() {
        var expression = _display.value
        if (expression.isEmpty() || screenShowsError(expression)) return
        if (lastCharIsOperator(expression)) {
            // Выражение не должно заканчиваться оператором: «12+» → «12»
            expression = expression.dropLast(1)
        }
        try {
            // «-5+2» без ведущего нуля парсить сложнее; получается «0-5+2»
            val normalized = if (expression.startsWith("-")) "0$expression" else expression
            val (numbers, operators) = splitIntoNumbersAndOperators(normalized)
            val (afterMulDiv, remainingOps) = doMultiplyAndDivide(numbers, operators)
            val result = doAddAndSubtract(afterMulDiv, remainingOps)
            setDisplay(formatResult(result))
        } catch (e: Exception) {
            setDisplay(e.message ?: "Error")
        }
    }

    /** Удаляет один символ с конца; если строка пуста — показывает «0». */
    private fun backspace() {
        val current = _display.value
        if (current.isNotEmpty() && current != DEFAULT_DISPLAY) {
            val shorter = current.dropLast(1)
            setDisplay(if (shorter.isEmpty()) DEFAULT_DISPLAY else shorter)
        }
    }

    /** Если на экране одно число, делит его на 100 (процент). */
    private fun applyPercent() {
        val current = _display.value
        if (current.isEmpty() || screenShowsError(current)) return
        val value = current.toDoubleOrNull() ?: return
        setDisplay(formatResult(value / 100.0))
    }

    /**
     * Слева направо разбирает выражение в два списка: числа по порядку и операторы по порядку.
     * Пример: `"12+3*4"` → `[12,3,4]` и `[+,*]`.
     */
    private fun splitIntoNumbersAndOperators(expression: String): Pair<List<Double>, List<Char>> {
        val numbers = mutableListOf<Double>()
        val operators = mutableListOf<Char>()
        var buildingNumber = StringBuilder()
        // После каждого оператора должно идти число (или минус в начале числа)
        var weNeedANumberNext = true

        for (ch in expression) {
            when {
                ch.isDigit() || ch == '.' -> {
                    buildingNumber.append(ch)
                    weNeedANumberNext = false
                }
                isOperatorChar(ch) -> {
                    if (weNeedANumberNext) {
                        // Минус внутри числа: часть «-5», всё ещё собираем число
                        if (ch == '-' && buildingNumber.isEmpty()) {
                            buildingNumber.append(ch)
                            continue
                        }
                        throw IllegalArgumentException("Invalid expression")
                    }
                    numbers.add(buildingNumber.toString().toDouble())
                    operators.add(ch)
                    buildingNumber = StringBuilder()
                    weNeedANumberNext = true
                }
                else -> throw IllegalArgumentException("Invalid character")
            }
        }

        if (buildingNumber.isNotEmpty()) {
            numbers.add(buildingNumber.toString().toDouble())
        }
        // Всегда: чисел на один больше, чем операторов
        if (numbers.size != operators.size + 1) {
            throw IllegalArgumentException("Invalid expression")
        }
        return numbers to operators
    }

    /**
     * Этап приоритета: выполняет только умножение и деление с правым операндом.
     * Сложение и вычитание переносятся в новые списки для следующего шага.
     */
    private fun doMultiplyAndDivide(
        numbers: List<Double>,
        operators: List<Char>
    ): Pair<List<Double>, List<Char>> {
        val newNumbers = mutableListOf(numbers[0])
        val newOperators = mutableListOf<Char>()

        for (i in operators.indices) {
            val op = operators[i]
            val right = numbers[i + 1]
            when (op) {
                '*' -> {
                    // Последнее число в новом списке — левый операнд умножения
                    val left = newNumbers.removeAt(newNumbers.lastIndex)
                    newNumbers.add(left * right)
                }
                '÷' -> {
                    val left = newNumbers.removeAt(newNumbers.lastIndex)
                    if (right == 0.0) throw ArithmeticException("Division by zero")
                    newNumbers.add(left / right)
                }
                else -> {
                    // + или −: пока только сохраняем число и оператор
                    newNumbers.add(right)
                    newOperators.add(op)
                }
            }
        }
        return newNumbers to newOperators
    }

    /**
     * Второй этап: только сложение и вычитание по укороченному списку чисел (слева направо).
     */
    private fun doAddAndSubtract(numbers: List<Double>, operators: List<Char>): Double {
        var total = numbers[0]
        for (i in operators.indices) {
            when (operators[i]) {
                '+' -> total += numbers[i + 1]
                '-' -> total -= numbers[i + 1]
            }
        }
        return total
    }

    /** Является ли символ одним из поддерживаемых операторов. */
    private fun isOperatorChar(ch: Char): Boolean = ch in "+-*÷"

    /** Последний символ строки — оператор (некорректное выражение или замена оператора). */
    private fun lastCharIsOperator(text: String): Boolean =
        text.isNotEmpty() && isOperatorChar(text.last())

    /**
     * Преобразует `Double` в строку для экрана: целые без «.0»;
     * иначе убирает лишние нули в конце дробной части.
     */
    private fun formatResult(result: Double): String {
        return if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            result.toString().trimEnd('0').trimEnd('.')
        }
    }

    /** Показывает ли текущий текст ошибку (чтобы остальные кнопки вели себя корректно). */
    private fun screenShowsError(str: String): Boolean {
        return str.contains("Invalid") || str == "Division by zero" || str.contains("Error")
    }
}
