package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class SessionItemId(val value: String) {
    companion object {
        fun generate(): SessionItemId = SessionItemId(generateIdValue())
    }
}
