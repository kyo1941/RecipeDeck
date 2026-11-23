package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeRecipeRepository : RecipeRepository {
    val savedRecipes = mutableListOf<Recipe>()
    var saveFailure: RecipeRepositoryFailure? = null
    var findFailure: RecipeRepositoryFailure? = null
    private val saveCount = MutableStateFlow(0)

    override suspend fun save(recipe: Recipe): Outcome<Unit, RecipeRepositoryFailure> {
        saveFailure?.let { return Outcome.Failure(it) }
        savedRecipes += recipe
        saveCount.update { it + 1 }
        return Outcome.Success(Unit)
    }

    override suspend fun findById(id: RecipeId): Outcome<Recipe?, RecipeRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(savedRecipes.lastOrNull { it.id == id })
    }

    override fun observeById(id: RecipeId): Flow<Outcome<Recipe?, RecipeRepositoryFailure>> =
        saveCount.map { findFailure?.let { Outcome.Failure(it) } ?: Outcome.Success(savedRecipes.lastOrNull { it.id == id }) }

    override fun observeAll(): Flow<Outcome<ImmutableSet<Recipe>, RecipeRepositoryFailure>> =
        saveCount.map {
            findFailure?.let { Outcome.Failure(it) } ?: Outcome.Success(savedRecipes.associateBy { it.id }.values.toImmutableSet())
        }
}
