package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.item.ItemReference

data class SessionItem(
    val id: SessionItemId,
    val item: ItemReference,
    val manualAddition: ManualAddition?,
)

data class ManualAddition(
    val amount: Amount?,
)
