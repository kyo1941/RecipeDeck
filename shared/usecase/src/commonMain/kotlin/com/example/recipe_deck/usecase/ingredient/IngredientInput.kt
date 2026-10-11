package com.example.recipe_deck.usecase.ingredient

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation

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
