package com.example.recipedeck.usecase.ingredient

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.Quantity
import com.example.recipedeck.domain.amount.QuantityNotation
import com.example.recipedeck.domain.item.ItemId

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

internal fun List<IngredientInput>.selectedItemIds(): Set<ItemId> =
    mapNotNull { (it.item as? ItemInput.Selected)?.itemId }.toSet()

internal fun List<IngredientInput>.typedDisplayNames(): Set<NotBlankString> =
    mapNotNull { (it.item as? ItemInput.Typed)?.let { typed -> NotBlankString.of(typed.text) } }.toSet()
