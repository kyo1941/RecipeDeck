package com.example.recipe_deck.usecase.ingredient

import com.example.recipe_deck.domain.item.ItemId

sealed interface ItemInput {
    data class Selected(val itemId: ItemId) : ItemInput
    data class Typed(val text: String) : ItemInput
}
