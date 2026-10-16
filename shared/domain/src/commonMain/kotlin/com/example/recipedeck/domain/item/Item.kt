package com.example.recipedeck.domain.item

import com.example.recipedeck.domain.NotBlankString

data class Item(
    val id: ItemId,
    val displayName: NotBlankString,
)
