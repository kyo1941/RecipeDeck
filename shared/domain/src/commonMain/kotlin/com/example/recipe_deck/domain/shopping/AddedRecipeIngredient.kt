package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.item.ItemId

data class AddedRecipeIngredient(
    val displayName: NotBlankString,
    val itemId: ItemId?,
    val amount: Amount?,
    val isExcluded: Boolean,
)
