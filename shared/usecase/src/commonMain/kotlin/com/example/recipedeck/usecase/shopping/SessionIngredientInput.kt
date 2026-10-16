package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.usecase.ingredient.IngredientInput

data class SessionIngredientInput(
    val ingredient: IngredientInput,
    val isExcluded: Boolean,
)
