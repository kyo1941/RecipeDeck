package com.example.recipedeck.domain.recipe

import com.example.recipedeck.domain.NotBlankString
import kotlinx.collections.immutable.ImmutableList

data class Recipe(
    val id: RecipeId,
    val name: NotBlankString,
    val status: RecipeStatus,
    val photoRef: String?,
    val memo: NotBlankString?,
    val ingredients: ImmutableList<RecipeIngredient>,
)
