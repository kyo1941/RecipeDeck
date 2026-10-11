package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount

data class SessionRecipeIngredient(
    val sessionItemId: SessionItemId,
    val amount: Amount?,
    val isExcluded: Boolean,
)
