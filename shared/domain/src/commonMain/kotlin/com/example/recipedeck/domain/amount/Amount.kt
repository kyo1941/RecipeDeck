package com.example.recipedeck.domain.amount

data class Amount(
    val quantity: Quantity,
    val notation: QuantityNotation,
    val unit: AmountUnit?,
) {
    companion object {
        fun ofOrNull(quantity: Quantity?, notation: QuantityNotation, unit: AmountUnit?): Amount? =
            quantity?.let { Amount(it, notation, unit) }
    }
}
