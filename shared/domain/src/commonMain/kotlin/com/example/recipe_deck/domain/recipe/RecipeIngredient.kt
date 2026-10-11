package com.example.recipe_deck.domain.recipe

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.item.ItemId

data class RecipeIngredient(
    val itemId: ItemId,
    val amount: Amount?,
)
