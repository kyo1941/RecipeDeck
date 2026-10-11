package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class SessionRecipeId(val value: String) {
    companion object {
        fun generate(): SessionRecipeId = SessionRecipeId(generateIdValue())
    }
}
