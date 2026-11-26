package com.example.recipedeck.domain.recipe

import com.example.recipedeck.domain.Outcome
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    suspend fun save(recipe: Recipe): Outcome<Unit, RecipeRepositoryFailure>
    suspend fun findById(id: RecipeId): Outcome<Recipe?, RecipeRepositoryFailure>
    fun observeAll(): Flow<Outcome<ImmutableSet<Recipe>, RecipeRepositoryFailure>>
    fun observeById(id: RecipeId): Flow<Outcome<Recipe?, RecipeRepositoryFailure>>
}
