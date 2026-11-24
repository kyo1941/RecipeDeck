package com.example.recipedeck.ui.amount

import com.example.recipedeck.domain.amount.Quantity
import com.example.recipedeck.domain.amount.QuantityNotation

internal fun Quantity.text(notation: QuantityNotation): String = when (notation) {
    QuantityNotation.FRACTION -> fractionText()
    QuantityNotation.DECIMAL -> decimalTextOrNull() ?: fractionText()
}

private val acceptableQuantityText = Regex("""[0-9]{0,4}(\.[0-9]{0,3})?|[0-9]{0,4}/[0-9]{0,4}""")

internal fun isAcceptableQuantityText(text: String): Boolean = acceptableQuantityText.matches(text)

internal fun notationOf(text: String): QuantityNotation =
    if ('/' in text) QuantityNotation.FRACTION else QuantityNotation.DECIMAL

internal fun quantityOrNull(text: String): Quantity? {
    if (!isAcceptableQuantityText(text)) return null
    return if ('/' in text) {
        val (numerator, denominator) = text.split('/')
        if (numerator.isEmpty() || denominator.isEmpty()) return null
        Quantity.of(numerator.toLong(), denominator.toLong())
    } else {
        val integerPart = text.substringBefore('.')
        val fractionPart = text.substringAfter('.', missingDelimiterValue = "")
        if (integerPart.isEmpty() && fractionPart.isEmpty()) return null
        var denominator = 1L
        repeat(fractionPart.length) { denominator *= 10 }
        Quantity.of((integerPart.ifEmpty { "0" } + fractionPart).toLong(), denominator)
    }
}

private fun Quantity.fractionText(): String = if (denominator == 1L) "$numerator" else "$numerator/$denominator"

/**
 * 小数で入力された数量は分母が 10 の累乗の約数になるので、割り切れる桁数を探して小数に戻す。
 * 計算の途中で桁があふれる数量は、小数に戻さず null を返す。
 */
private fun Quantity.decimalTextOrNull(): String? {
    var scale = 1L
    var digits = 0
    while (scale % denominator != 0L) {
        if (scale > Long.MAX_VALUE / 10) return null
        scale *= 10
        digits++
    }
    val multiplier = scale / denominator
    if (numerator > Long.MAX_VALUE / multiplier) return null
    val scaled = numerator * multiplier
    if (digits == 0) return "$scaled"
    val integerPart = scaled / scale
    val fractionPart = (scaled % scale).toString().padStart(digits, '0').trimEnd('0')
    return "$integerPart.$fractionPart"
}
