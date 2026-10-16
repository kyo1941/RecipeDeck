package com.example.recipedeck.domain.recipe

import com.example.recipedeck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class RecipeId(val value: String) {
    companion object {
        fun generate(): RecipeId = RecipeId(generateIdValue())
    }
}
