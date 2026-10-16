package com.example.recipedeck.usecase.ingredient

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.Quantity
import com.example.recipedeck.domain.amount.QuantityNotation

data class IngredientInput(
    val item: ItemInput,
    val quantity: Quantity?,
    val notation: QuantityNotation,
    val unit: AmountUnit?,
) {
    val amount: Amount? get() = Amount.ofOrNull(quantity, notation, unit)

    val isInvalid: Boolean get() = hasNoDisplayName && amount != null

    private val hasNoDisplayName: Boolean
        get() = item is ItemInput.Typed && NotBlankString.of(item.text) == null
}
