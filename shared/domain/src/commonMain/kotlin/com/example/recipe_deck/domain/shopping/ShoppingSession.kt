@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.amount.Multiplier
import com.example.recipe_deck.domain.recipe.RecipeId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ConsistentCopyVisibility
data class ShoppingSession private constructor(
    val id: ShoppingSessionId,
    val state: SessionState,
    val createdAt: Instant,
    val recipes: ImmutableList<SessionRecipe>,
    val items: ImmutableList<SessionItem>,
) {
    fun withAddedRecipe(
        sourceRecipeId: RecipeId,
        recipeName: NotBlankString,
        multiplier: Multiplier,
        ingredients: List<AddedRecipeIngredient>,
    ): ShoppingSession {
        val sessionItems = items.associateByTo(mutableMapOf()) { it.item }
        val recipeIngredients = ingredients.map { ingredient ->
            val sessionItem = sessionItems.getOrPut(ingredient.item) {
                SessionItem(SessionItemId.generate(), ingredient.item, manualAddition = null)
            }
            SessionRecipeIngredient(
                sessionItemId = sessionItem.id,
                amount = ingredient.amount,
                isExcluded = ingredient.isExcluded,
            )
        }
        val sessionRecipe = SessionRecipe(
            id = SessionRecipeId.generate(),
            sourceRecipeId = sourceRecipeId,
            recipeName = recipeName,
            multiplier = multiplier,
            ingredients = recipeIngredients.toImmutableList(),
        )
        return copy(
            recipes = (recipes + sessionRecipe).toImmutableList(),
            items = sessionItems.values.toImmutableList(),
        )
    }

    companion object {
        fun create(createdAt: Instant): ShoppingSession = ShoppingSession(
            id = ShoppingSessionId.generate(),
            state = SessionState.Active,
            createdAt = createdAt,
            recipes = persistentListOf(),
            items = persistentListOf(),
        )
    }
}
