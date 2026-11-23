package com.example.recipedeck.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object RecipeList : Route

    @Serializable
    data object ShoppingList : Route

    @Serializable
    data class Recipe(val recipeId: String) : Route
}
