package com.example.recipedeck.ui

import com.example.recipedeck.ui.recipe.RecipeListViewModel
import com.example.recipedeck.ui.shopping.ShoppingListViewModel

interface ScreenViewModels {
    val recipeListViewModel: RecipeListViewModel
    val shoppingListViewModel: ShoppingListViewModel
}
