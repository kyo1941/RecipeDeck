package com.example.recipedeck.ui

import com.example.recipedeck.ui.recipe.CreateRecipeViewModel
import com.example.recipedeck.ui.recipe.EditRecipeViewModelFactory
import com.example.recipedeck.ui.recipe.RecipeListViewModel
import com.example.recipedeck.ui.recipe.RecipeViewModelFactory
import com.example.recipedeck.ui.shopping.ShoppingListViewModel

interface ScreenViewModels {
    val recipeListViewModel: RecipeListViewModel
    val shoppingListViewModel: ShoppingListViewModel
    val recipeViewModelFactory: RecipeViewModelFactory
    val createRecipeViewModel: CreateRecipeViewModel
    val editRecipeViewModelFactory: EditRecipeViewModelFactory
}
