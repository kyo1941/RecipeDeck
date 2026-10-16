package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.amount.Amount

data class SessionRecipeIngredient(
    val id: ShoppingListItemSourceId,
    val sessionItemId: SessionItemId,
    val amount: Amount?,
    val isExcluded: Boolean,
)
