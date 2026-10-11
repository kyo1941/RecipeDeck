package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.item.ItemReference

data class AddedRecipeIngredient(
    val item: ItemReference,
    val amount: Amount?,
    val isExcluded: Boolean,
)
