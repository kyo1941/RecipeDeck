package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class ShoppingListItemSourceId(val value: String) {
    companion object {
        fun generate(): ShoppingListItemSourceId = ShoppingListItemSourceId(generateIdValue())
    }
}
