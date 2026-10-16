package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.item.ItemId

data class AddedRecipeIngredient(
    val displayName: NotBlankString,
    val itemId: ItemId?,
    val amount: Amount?,
    val isExcluded: Boolean,
)
