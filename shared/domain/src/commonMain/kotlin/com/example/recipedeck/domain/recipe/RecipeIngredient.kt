package com.example.recipedeck.domain.recipe

import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.item.ItemId

data class RecipeIngredient(
    val itemId: ItemId,
    val amount: Amount?,
)
