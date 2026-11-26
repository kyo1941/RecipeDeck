package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun interface ObserveRecipeListUseCase {
    operator fun invoke(): Flow<ObserveRecipeListResult>
}

fun ObserveRecipeListUseCase(
    recipeRepository: RecipeRepository,
): ObserveRecipeListUseCase = object : ObserveRecipeListUseCase {
    override fun invoke(): Flow<ObserveRecipeListResult> =
        recipeRepository.observeAll().map { found ->
            when (found) {
                is Outcome.Success -> ObserveRecipeListResult.Listed(found.value)
                is Outcome.Failure -> found.error.toObserveRecipeListResult()
            }
        }

    private fun RecipeRepositoryFailure.toObserveRecipeListResult(): ObserveRecipeListResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> ObserveRecipeListResult.Unexpected(cause)
    }
}

sealed interface ObserveRecipeListResult {
    data class Listed(val recipes: ImmutableSet<Recipe>) : ObserveRecipeListResult
    data class Unexpected(val cause: Throwable) : ObserveRecipeListResult
}
