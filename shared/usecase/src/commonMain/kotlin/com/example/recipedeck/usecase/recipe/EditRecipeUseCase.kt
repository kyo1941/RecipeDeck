package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.TransactionRunner
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.ingredient.IngredientInput
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class EditRecipeUseCase(
    private val recipeRepository: RecipeRepository,
    private val itemRepository: ItemRepository,
    private val transactionRunner: TransactionRunner,
) {
    suspend operator fun invoke(
        recipeId: RecipeId,
        name: String,
        status: RecipeStatus,
        memo: String,
        ingredients: List<IngredientInput>,
    ): EditRecipeResult {
        val recipeName = NotBlankString.of(name) ?: return EditRecipeResult.NameRequired

        ingredients.indices
            .filter { ingredients[it].isInvalid }
            .takeIf { it.isNotEmpty() }
            ?.let { return EditRecipeResult.IngredientDisplayNameRequired(it.toImmutableList()) }

        return transactionRunner.run {
            val recipe = when (val found = recipeRepository.findById(recipeId)) {
                is Outcome.Success -> found.value ?: rollback(EditRecipeResult.RecipeNoLongerExists)
                is Outcome.Failure -> rollback(found.error.toEditRecipeResult())
            }

            val existingItems = when (val found = itemRepository.findAll()) {
                is Outcome.Success -> found.value.associateBy { it.displayName }
                is Outcome.Failure -> rollback(found.error.toEditRecipeResult())
            }

            val resolved = resolveIngredients(ingredients, existingItems)

            resolved.newItems.forEach { item ->
                when (val saved = itemRepository.save(item)) {
                    is Outcome.Success -> Unit
                    is Outcome.Failure -> rollback(saved.error.toEditRecipeResult())
                }
            }

            val editedRecipe = recipe.copy(
                name = recipeName,
                status = status,
                memo = NotBlankString.of(memo),
                ingredients = resolved.recipeIngredients,
            )
            when (val saved = recipeRepository.save(editedRecipe)) {
                is Outcome.Success -> EditRecipeResult.Saved(resolved.matchedExistingItems)
                is Outcome.Failure -> rollback(saved.error.toEditRecipeResult())
            }
        }
    }

    private fun RecipeRepositoryFailure.toEditRecipeResult(): EditRecipeResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> EditRecipeResult.Unexpected(cause)
    }

    private fun ItemRepositoryFailure.toEditRecipeResult(): EditRecipeResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> EditRecipeResult.Unexpected(cause)
    }
}

sealed interface EditRecipeResult {
    data class Saved(val matchedExistingItems: ImmutableList<Item>) : EditRecipeResult
    data object NameRequired : EditRecipeResult
    data class IngredientDisplayNameRequired(val ingredientIndexes: ImmutableList<Int>) : EditRecipeResult
    data object RecipeNoLongerExists : EditRecipeResult
    data class Unexpected(val cause: Throwable) : EditRecipeResult
}
