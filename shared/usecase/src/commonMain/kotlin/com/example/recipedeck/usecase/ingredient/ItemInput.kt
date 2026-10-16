package com.example.recipedeck.usecase.ingredient

import com.example.recipedeck.domain.item.ItemId

sealed interface ItemInput {
    data class Selected(val itemId: ItemId) : ItemInput
    data class Typed(val text: String) : ItemInput
}
