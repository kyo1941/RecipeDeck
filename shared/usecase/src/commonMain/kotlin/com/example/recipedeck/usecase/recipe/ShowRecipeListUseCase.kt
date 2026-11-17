package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShowRecipeListUseCase(
    private val recipeRepository: RecipeRepository,
) {
    operator fun invoke(): Flow<ShowRecipeListResult> =
        recipeRepository.observeAll().map { found ->
            when (found) {
                is Outcome.Success -> ShowRecipeListResult.Shown(found.value)
                is Outcome.Failure -> found.error.toShowRecipeListResult()
            }
        }

    private fun RecipeRepositoryFailure.toShowRecipeListResult(): ShowRecipeListResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> ShowRecipeListResult.Unexpected(cause)
    }
}

sealed interface ShowRecipeListResult {
    data class Shown(val recipes: ImmutableSet<Recipe>) : ShowRecipeListResult
    data class Unexpected(val cause: Throwable) : ShowRecipeListResult
}
