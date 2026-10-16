package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.amount.Multiplier
import com.example.recipedeck.domain.recipe.RecipeId
import kotlinx.collections.immutable.ImmutableList

data class SessionRecipe(
    val id: SessionRecipeId,
    val sourceRecipeId: RecipeId,
    val recipeName: NotBlankString,
    val multiplier: Multiplier,
    val ingredients: ImmutableList<SessionRecipeIngredient>,
)
