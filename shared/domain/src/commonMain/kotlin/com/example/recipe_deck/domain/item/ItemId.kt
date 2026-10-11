package com.example.recipe_deck.domain.item

import com.example.recipe_deck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class ItemId(val value: String) {
    companion object {
        fun generate(): ItemId = ItemId(generateIdValue())
    }
}
