@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.amount.Multiplier
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemRepository
import com.example.recipe_deck.domain.item.ItemRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.recipe.RecipeRepository
import com.example.recipe_deck.domain.recipe.RecipeRepositoryFailure
import com.example.recipe_deck.domain.recipe.RecipeStatus
import com.example.recipe_deck.domain.shopping.AddedRecipeIngredient
import com.example.recipe_deck.domain.shopping.ShoppingSession
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepository
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepositoryFailure
import com.example.recipe_deck.usecase.ingredient.ItemInput
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AddRecipeToShoppingSessionUseCase(
    private val recipeRepository: RecipeRepository,
    private val itemRepository: ItemRepository,
    private val shoppingSessionRepository: ShoppingSessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        recipeId: RecipeId,
        multiplier: Multiplier,
        ingredients: List<SessionIngredientInput>,
    ): AddRecipeToShoppingSessionResult {
        ingredients.indices
            .filter { ingredients[it].ingredient.isInvalid }
            .takeIf { it.isNotEmpty() }
            ?.let { return AddRecipeToShoppingSessionResult.IngredientDisplayNameRequired(it.toImmutableList()) }

        val recipe = when (val found = recipeRepository.findById(recipeId)) {
            is Outcome.Success -> found.value ?: return AddRecipeToShoppingSessionResult.RecipeNoLongerExists
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
        }
        if (recipe.status == RecipeStatus.DRAFT) return AddRecipeToShoppingSessionResult.RecipeIsDraft

        val existingItems = when (val found = itemRepository.findAll()) {
            is Outcome.Success -> found.value
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
        }
        val existingItemsById = existingItems.associateBy { it.id }
        val existingItemsByDisplayName = existingItems.associateBy { it.displayName }

        ingredients.indices
            .filter { index ->
                val item = ingredients[index].ingredient.item
                item is ItemInput.Selected && item.itemId !in existingItemsById
            }
            .takeIf { it.isNotEmpty() }
            ?.let { return AddRecipeToShoppingSessionResult.SelectedItemNoLongerExists(it.toImmutableList()) }

        val session = when (val found = shoppingSessionRepository.findActive()) {
            is Outcome.Success -> found.value ?: ShoppingSession.create(clock.now())
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
        }

        val matchedExistingItems = mutableSetOf<Item>()
        val addedIngredients = ingredients.mapNotNull { input ->
            val (displayName, itemId) = when (val inputItem = input.ingredient.item) {
                is ItemInput.Selected -> {
                    val item = existingItemsById[inputItem.itemId] ?: return@mapNotNull null
                    item.displayName to item.id
                }
                is ItemInput.Typed -> {
                    val displayName = NotBlankString.of(inputItem.text) ?: return@mapNotNull null
                    val existingItem = existingItemsByDisplayName[displayName]?.also { matchedExistingItems += it }
                    displayName to existingItem?.id
                }
            }
            AddedRecipeIngredient(displayName, itemId, input.ingredient.amount, input.isExcluded)
        }

        val updatedSession = session.withAddedRecipe(recipe.id, recipe.name, multiplier, addedIngredients)
        return when (val saved = shoppingSessionRepository.save(updatedSession)) {
            is Outcome.Success -> AddRecipeToShoppingSessionResult.Added(matchedExistingItems.toImmutableList())
            is Outcome.Failure -> saved.error.toAddRecipeToShoppingSessionResult()
        }
    }

    private fun RecipeRepositoryFailure.toAddRecipeToShoppingSessionResult(): AddRecipeToShoppingSessionResult =
        when (this) {
            is RecipeRepositoryFailure.Unexpected -> AddRecipeToShoppingSessionResult.Unexpected(cause)
        }

    private fun ItemRepositoryFailure.toAddRecipeToShoppingSessionResult(): AddRecipeToShoppingSessionResult =
        when (this) {
            is ItemRepositoryFailure.Unexpected -> AddRecipeToShoppingSessionResult.Unexpected(cause)
        }

    private fun ShoppingSessionRepositoryFailure.toAddRecipeToShoppingSessionResult(): AddRecipeToShoppingSessionResult =
        when (this) {
            is ShoppingSessionRepositoryFailure.Unexpected -> AddRecipeToShoppingSessionResult.Unexpected(cause)
        }
}

sealed interface AddRecipeToShoppingSessionResult {
    data class Added(val matchedExistingItems: ImmutableList<Item>) : AddRecipeToShoppingSessionResult
    data object RecipeIsDraft : AddRecipeToShoppingSessionResult
    data object RecipeNoLongerExists : AddRecipeToShoppingSessionResult
    data class SelectedItemNoLongerExists(val ingredientIndexes: ImmutableList<Int>) : AddRecipeToShoppingSessionResult
    data class IngredientDisplayNameRequired(val ingredientIndexes: ImmutableList<Int>) : AddRecipeToShoppingSessionResult
    data class Unexpected(val cause: Throwable) : AddRecipeToShoppingSessionResult
}
