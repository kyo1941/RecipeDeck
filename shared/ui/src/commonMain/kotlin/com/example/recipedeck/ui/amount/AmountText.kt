package com.example.recipedeck.ui.amount

import androidx.compose.runtime.Composable
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.Quantity
import com.example.recipedeck.domain.amount.QuantityNotation
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.amount_unit_gram
import com.example.recipedeck.ui.resources.amount_unit_kilogram
import com.example.recipedeck.ui.resources.amount_unit_liter
import com.example.recipedeck.ui.resources.amount_unit_milliliter
import com.example.recipedeck.ui.resources.amount_unit_piece
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun amountText(amount: Amount): String {
    val quantity = when (amount.notation) {
        QuantityNotation.FRACTION -> amount.quantity.fractionText()
        QuantityNotation.DECIMAL -> amount.quantity.decimalTextOrNull() ?: amount.quantity.fractionText()
    }
    return quantity + amount.unit?.let { unitText(it) }.orEmpty()
}

@Composable
private fun unitText(unit: AmountUnit): String = stringResource(
    when (unit) {
        AmountUnit.PIECE -> Res.string.amount_unit_piece
        AmountUnit.GRAM -> Res.string.amount_unit_gram
        AmountUnit.KILOGRAM -> Res.string.amount_unit_kilogram
        AmountUnit.MILLILITER -> Res.string.amount_unit_milliliter
        AmountUnit.LITER -> Res.string.amount_unit_liter
    },
)

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
