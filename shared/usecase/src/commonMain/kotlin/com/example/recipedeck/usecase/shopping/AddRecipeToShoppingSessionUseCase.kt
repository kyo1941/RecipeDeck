package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.amount.Multiplier
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.domain.shopping.AddedRecipeIngredient
import com.example.recipedeck.domain.shopping.ShoppingSession
import com.example.recipedeck.domain.shopping.ShoppingSessionRepository
import com.example.recipedeck.domain.shopping.ShoppingSessionRepositoryFailure
import com.example.recipedeck.usecase.ingredient.ItemInput
import com.example.recipedeck.usecase.ingredient.selectedItemIds
import com.example.recipedeck.usecase.ingredient.typedDisplayNames
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Clock

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

        val ingredientInputs = ingredients.map { it.ingredient }
        val existingItemsById = when (val found = itemRepository.findByIds(ingredientInputs.selectedItemIds())) {
            is Outcome.Success -> found.value.associateBy { it.id }
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
        }
        val existingItemsByDisplayName = when (val found = itemRepository.findByDisplayNames(ingredientInputs.typedDisplayNames())) {
            is Outcome.Success -> found.value.associateBy { it.displayName }
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
        }

        val matchedExistingItems = mutableSetOf<Item>()
        val missingSelectedIndexes = mutableListOf<Int>()
        val addedIngredients = ingredients.mapIndexedNotNull { index, input ->
            val (displayName, itemId) = when (val inputItem = input.ingredient.item) {
                is ItemInput.Selected -> {
                    val selectedItem = existingItemsById[inputItem.itemId]
                    if (selectedItem == null) {
                        missingSelectedIndexes += index
                        return@mapIndexedNotNull null
                    }
                    selectedItem.displayName to selectedItem.id
                }
                is ItemInput.Typed -> {
                    val displayName = NotBlankString.of(inputItem.text) ?: return@mapIndexedNotNull null
                    val existingItem = existingItemsByDisplayName[displayName]?.also { matchedExistingItems += it }
                    displayName to existingItem?.id
                }
            }
            AddedRecipeIngredient(displayName, itemId, input.ingredient.amount, input.isExcluded)
        }
        missingSelectedIndexes
            .takeIf { it.isNotEmpty() }
            ?.let { return AddRecipeToShoppingSessionResult.SelectedItemNoLongerExists(it.toImmutableList()) }

        val session = when (val found = shoppingSessionRepository.findActive()) {
            is Outcome.Success -> found.value ?: ShoppingSession.create(clock.now())
            is Outcome.Failure -> return found.error.toAddRecipeToShoppingSessionResult()
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
