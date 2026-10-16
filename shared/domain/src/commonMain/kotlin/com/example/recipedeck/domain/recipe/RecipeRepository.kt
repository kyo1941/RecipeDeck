package com.example.recipedeck.domain.recipe

import com.example.recipedeck.domain.Outcome

interface RecipeRepository {
    suspend fun save(recipe: Recipe): Outcome<Unit, RecipeRepositoryFailure>
    suspend fun findById(id: RecipeId): Outcome<Recipe?, RecipeRepositoryFailure>
}
