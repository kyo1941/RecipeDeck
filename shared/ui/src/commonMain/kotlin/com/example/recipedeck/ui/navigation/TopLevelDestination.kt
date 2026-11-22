package com.example.recipedeck.ui.navigation

import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.navigation_recipes
import com.example.recipedeck.ui.resources.navigation_shopping_list
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class TopLevelDestination(val root: Route, val label: StringResource) {
    RECIPES(Route.RecipeList, Res.string.navigation_recipes),
    SHOPPING_LIST(Route.ShoppingList, Res.string.navigation_shopping_list),
    ;

    companion object {
        val START = RECIPES
    }
}
