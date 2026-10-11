package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class ShoppingSessionId(val value: String) {
    companion object {
        fun generate(): ShoppingSessionId = ShoppingSessionId(generateIdValue())
    }
}
