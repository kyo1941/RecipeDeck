package com.example.recipe_deck.domain.item

import com.example.recipe_deck.domain.NotBlankString

sealed interface ItemReference {
    data class Registered(val itemId: ItemId) : ItemReference
    data class Unregistered(val displayName: NotBlankString) : ItemReference
}
