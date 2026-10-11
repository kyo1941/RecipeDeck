package com.example.recipe_deck.domain.recipe

import com.example.recipe_deck.domain.NotBlankString
import kotlinx.collections.immutable.ImmutableList

data class Recipe(
    val id: RecipeId,
    val name: NotBlankString,
    val status: RecipeStatus,
    val photoRef: String?,
    val memo: NotBlankString?,
    val ingredients: ImmutableList<RecipeIngredient>,
)
