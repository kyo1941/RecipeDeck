package com.example.recipe_deck.domain.item

import com.example.recipe_deck.domain.NotBlankString

data class Item(
    val id: ItemId,
    val displayName: NotBlankString,
)
