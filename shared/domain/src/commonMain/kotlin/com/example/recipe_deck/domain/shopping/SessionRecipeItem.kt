package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount

data class SessionRecipeItem(
    val sessionItemId: SessionItemId,
    val amount: Amount?,
    val isExcluded: Boolean,
)
