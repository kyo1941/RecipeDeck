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

fun interface ShowRecipeUseCase {
    operator fun invoke(recipeId: RecipeId): Flow<ShowRecipeResult>
}

fun ShowRecipeUseCase(
    recipeRepository: RecipeRepository,
    itemRepository: ItemRepository,
): ShowRecipeUseCase = object : ShowRecipeUseCase {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(recipeId: RecipeId): Flow<ShowRecipeResult> =
        recipeRepository.observeById(recipeId).flatMapLatest { found ->
            when (found) {
                is Outcome.Success -> found.value?.let(::observeWithItems) ?: flowOf(ShowRecipeResult.RecipeNoLongerExists)
                is Outcome.Failure -> flowOf(found.error.toShowRecipeResult())
            }
        }

    private fun observeWithItems(recipe: Recipe): Flow<ShowRecipeResult> =
        itemRepository.observeByIds(recipe.ingredients.map { it.itemId }.toSet()).map { found ->
            when (found) {
                is Outcome.Success -> recipe.toShown(found.value.associateBy { it.id })
                is Outcome.Failure -> found.error.toShowRecipeResult()
            }
        }

    private fun Recipe.toShown(itemsById: Map<ItemId, Item>): ShowRecipeResult {
        val ingredients = ingredients.map { ingredient ->
            val item = itemsById[ingredient.itemId]
                ?: return ShowRecipeResult.Unexpected(
                    IllegalStateException("Recipe ${id.value} の材料が指す Item ${ingredient.itemId.value} が見つからない"),
                )
            ShowRecipeResult.Shown.Ingredient(item, ingredient.amount)
        }
        return ShowRecipeResult.Shown(this, ingredients.toImmutableList())
    }

    private fun RecipeRepositoryFailure.toShowRecipeResult(): ShowRecipeResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> ShowRecipeResult.Unexpected(cause)
    }

    private fun ItemRepositoryFailure.toShowRecipeResult(): ShowRecipeResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> ShowRecipeResult.Unexpected(cause)
    }
}

sealed interface ShowRecipeResult {
    data class Shown(val recipe: Recipe, val ingredients: ImmutableList<Ingredient>) : ShowRecipeResult {
        data class Ingredient(val item: Item, val amount: Amount?)
    }
    data object RecipeNoLongerExists : ShowRecipeResult
    data class Unexpected(val cause: Throwable) : ShowRecipeResult
}
