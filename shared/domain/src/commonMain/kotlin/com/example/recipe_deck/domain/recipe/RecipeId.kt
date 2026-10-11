package com.example.recipe_deck.domain.recipe

import com.example.recipe_deck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class RecipeId(val value: String) {
    companion object {
        fun generate(): RecipeId = RecipeId(generateIdValue())
    }
}
