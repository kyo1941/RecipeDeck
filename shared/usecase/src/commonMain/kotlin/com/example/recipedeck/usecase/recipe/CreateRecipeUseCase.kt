package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.TransactionRunner
import com.example.recipedeck.domain.TransactionRunnerFailure
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.ingredient.IngredientInput
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class CreateRecipeUseCase(
    private val recipeRepository: RecipeRepository,
    private val itemRepository: ItemRepository,
    private val transactionRunner: TransactionRunner,
) {
    suspend operator fun invoke(
        name: String,
        status: RecipeStatus,
        ingredients: List<IngredientInput>,
    ): CreateRecipeResult {
        val recipeName = NotBlankString.of(name) ?: return CreateRecipeResult.NameRequired

        ingredients.indices
            .filter { ingredients[it].isInvalid }
            .takeIf { it.isNotEmpty() }
            ?.let { return CreateRecipeResult.IngredientDisplayNameRequired(it.toImmutableList()) }

        val outcome = transactionRunner.run {
            val existingItems = when (val found = itemRepository.findAll()) {
                is Outcome.Success -> found.value.associateBy { it.displayName }
                is Outcome.Failure -> rollback(found.error.toCreateRecipeResult())
            }

            val resolved = resolveIngredients(ingredients, existingItems)

            resolved.newItems.forEach { item ->
                when (val saved = itemRepository.save(item)) {
                    is Outcome.Success -> Unit
                    is Outcome.Failure -> rollback(saved.error.toCreateRecipeResult())
                }
            }

            val recipe = Recipe(
                id = RecipeId.generate(),
                name = recipeName,
                status = status,
                photoRef = null,
                memo = null,
                ingredients = resolved.recipeIngredients,
            )
            when (val saved = recipeRepository.save(recipe)) {
                is Outcome.Success -> CreateRecipeResult.Created(recipe.id, resolved.matchedExistingItems)
                is Outcome.Failure -> rollback(saved.error.toCreateRecipeResult())
            }
        }
        return when (outcome) {
            is Outcome.Success -> outcome.value
            is Outcome.Failure -> outcome.error.toCreateRecipeResult()
        }
    }

    private fun RecipeRepositoryFailure.toCreateRecipeResult(): CreateRecipeResult = when (this) {
        is RecipeRepositoryFailure.Unexpected -> CreateRecipeResult.Unexpected(cause)
    }

    private fun ItemRepositoryFailure.toCreateRecipeResult(): CreateRecipeResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> CreateRecipeResult.Unexpected(cause)
    }

    private fun TransactionRunnerFailure.toCreateRecipeResult(): CreateRecipeResult = when (this) {
        is TransactionRunnerFailure.Unexpected -> CreateRecipeResult.Unexpected(cause)
    }
}

sealed interface CreateRecipeResult {
    data class Created(val recipeId: RecipeId, val matchedExistingItems: ImmutableList<Item>) : CreateRecipeResult
    data object NameRequired : CreateRecipeResult
    data class IngredientDisplayNameRequired(val ingredientIndexes: ImmutableList<Int>) : CreateRecipeResult
    data class Unexpected(val cause: Throwable) : CreateRecipeResult
}
