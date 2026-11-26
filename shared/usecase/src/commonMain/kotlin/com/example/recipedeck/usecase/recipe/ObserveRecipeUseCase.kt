package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

fun interface ObserveRecipeUseCase {
    operator fun invoke(recipeId: RecipeId): Flow<ObserveRecipeResult>
}

fun ObserveRecipeUseCase(
    recipeRepository: RecipeRepository,
    itemRepository: ItemRepository,
): ObserveRecipeUseCase = object : ObserveRecipeUseCase {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(recipeId: RecipeId): Flow<ObserveRecipeResult> =
        recipeRepository.observeById(recipeId).flatMapLatest { found ->
            when (found) {
                is Outcome.Success -> found.value?.let(::observeWithItems) ?: flowOf(ObserveRecipeResult.RecipeNoLongerExists)
                is Outcome.Failure -> flowOf(found.error.toObserveRecipeResult())
            }
        }

    private fun observeWithItems(recipe: Recipe): Flow<ObserveRecipeResult> =
        itemRepository.observeByIds(recipe.ingredients.map { it.itemId }.toSet()).map { found ->
            when (found) {
                is Outcome.Success -> recipe.toFound(found.value.associateBy { it.id })
                is Outcome.Failure -> found.error.toObserveRecipeResult()
            }
        }

    private fun Recipe.toFound(itemsById: Map<ItemId, Item>): ObserveRecipeResult {
        val ingredients = ingredients.map { ingredient ->
            val item = itemsById[ingredient.itemId]
                ?: return ObserveRecipeResult.Unexpected(
                    IllegalStateException("Recipe ${id.value} の材料が指す Item ${ingredient.itemId.value} が見つからない"),
                )
            ObserveRecipeResult.Found.Ingredient(item, ingredient.amount)
        }
        return ObserveRecipeResult.Found(this, ingredients.toImmutableList())
    }

    private fun RecipeRepositoryFailure.toObserveRecipeResult(): ObserveRecipeResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> ObserveRecipeResult.Unexpected(cause)
    }

    private fun ItemRepositoryFailure.toObserveRecipeResult(): ObserveRecipeResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> ObserveRecipeResult.Unexpected(cause)
    }
}

sealed interface ObserveRecipeResult {
    data class Found(val recipe: Recipe, val ingredients: ImmutableList<Ingredient>) : ObserveRecipeResult {
        data class Ingredient(val item: Item, val amount: Amount?)
    }
    data object RecipeNoLongerExists : ObserveRecipeResult
    data class Unexpected(val cause: Throwable) : ObserveRecipeResult
}
