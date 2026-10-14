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
    /** Session は Item から独立したスナップショットなので、同じ品目かどうかを、Session が持つ表示名と Item へのつながりの組だけで決める。 */
    fun withAddedRecipe(
        sourceRecipeId: RecipeId,
        recipeName: NotBlankString,
        multiplier: Multiplier,
        ingredients: List<AddedRecipeIngredient>,
    ): ShoppingSession {
        val sessionItems = items.associateByTo(mutableMapOf()) { it.displayName to it.itemId }
        val recipeIngredients = ingredients.map { ingredient ->
            val sessionItem = sessionItems.getOrPut(ingredient.displayName to ingredient.itemId) {
                SessionItem(SessionItemId.generate(), ingredient.displayName, ingredient.itemId, manualAddition = null)
            }
            SessionRecipeIngredient(
                id = ShoppingListItemSourceId.generate(),
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

    fun toShoppingList(): ImmutableList<ShoppingListItem> {
        val recipeSources = recipes
            .flatMap { recipe ->
                recipe.ingredients.map { ingredient ->
                    ingredient.sessionItemId to ShoppingListItemSource.FromRecipe(
                        id = ingredient.id,
                        recipeName = recipe.recipeName,
                        multiplier = recipe.multiplier,
                        amount = ingredient.amount,
                        isExcluded = ingredient.isExcluded,
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
        return items
            .map { item ->
                val manualSource = item.manualAddition?.let { ShoppingListItemSource.Manual(it.id, it.amount) }
                ShoppingListItem(
                    sessionItemId = item.id,
                    displayName = item.displayName,
                    itemId = item.itemId,
                    sources = (recipeSources[item.id].orEmpty() + listOfNotNull(manualSource)).toImmutableList(),
                )
            }
            .toImmutableList()
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
