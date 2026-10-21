package com.example.recipedeck.data.amount

import androidx.room3.ColumnInfo
import com.example.recipedeck.data.InvalidStoredRecordException
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.Quantity
import com.example.recipedeck.domain.amount.QuantityNotation

internal data class AmountColumns(
    @ColumnInfo(name = "quantity_numerator") val quantityNumerator: Long?,
    @ColumnInfo(name = "quantity_denominator") val quantityDenominator: Long?,
    @ColumnInfo(name = "quantity_notation") val quantityNotation: String?,
    @ColumnInfo(name = "unit") val unit: String?,
)

internal fun AmountColumns.toAmountOrNull(): Amount? {
    if (quantityNumerator == null && quantityDenominator == null && quantityNotation == null && unit == null) {
        return null
    }
    if (quantityNumerator == null || quantityDenominator == null || quantityNotation == null) {
        throw InvalidStoredRecordException("分量の列の一部だけが空になっている: $this")
    }
    return Amount(
        quantity = Quantity.of(quantityNumerator, quantityDenominator)
            ?: throw InvalidStoredRecordException("数量が正の数になっていない: $this"),
        notation = quantityNotationOf(quantityNotation),
        unit = unit?.let(::amountUnitOf),
    )
}

internal fun Amount?.toAmountColumns(): AmountColumns = AmountColumns(
    quantityNumerator = this?.quantity?.numerator,
    quantityDenominator = this?.quantity?.denominator,
    quantityNotation = this?.notation?.storedValue,
    unit = this?.unit?.storedValue,
)

private val QuantityNotation.storedValue: String
    get() = when (this) {
        QuantityNotation.FRACTION -> "fraction"
        QuantityNotation.DECIMAL -> "decimal"
    }

private val AmountUnit.storedValue: String
    get() = when (this) {
        AmountUnit.PIECE -> "piece"
        AmountUnit.GRAM -> "gram"
        AmountUnit.KILOGRAM -> "kilogram"
        AmountUnit.MILLILITER -> "milliliter"
        AmountUnit.LITER -> "liter"
    }

private fun quantityNotationOf(value: String): QuantityNotation =
    QuantityNotation.entries.firstOrNull { it.storedValue == value }
        ?: throw InvalidStoredRecordException("知らない入力形式: $value")

private fun amountUnitOf(value: String): AmountUnit =
    AmountUnit.entries.firstOrNull { it.storedValue == value }
        ?: throw InvalidStoredRecordException("知らない単位: $value")
