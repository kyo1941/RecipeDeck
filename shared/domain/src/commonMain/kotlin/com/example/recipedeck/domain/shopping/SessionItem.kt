package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.item.ItemId

data class SessionItem(
    val id: SessionItemId,
    val displayName: NotBlankString,
    val itemId: ItemId?,
    val manualAddition: ManualAddition?,
)

data class ManualAddition(
    val id: ShoppingListItemSourceId,
    val amount: Amount?,
)
