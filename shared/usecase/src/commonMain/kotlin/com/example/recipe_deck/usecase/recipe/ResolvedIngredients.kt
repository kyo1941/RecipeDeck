package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.recipe.RecipeIngredient
import com.example.recipe_deck.usecase.ingredient.IngredientInput
import com.example.recipe_deck.usecase.ingredient.ItemInput
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class ResolvedIngredients(
    val recipeIngredients: ImmutableList<RecipeIngredient>,
    val newItems: ImmutableList<Item>,
    val matchedExistingItems: ImmutableList<Item>,
)

internal fun resolveIngredients(
    ingredients: List<IngredientInput>,
    existingItems: Map<NotBlankString, Item>,
): ResolvedIngredients {
    val newItems = mutableMapOf<NotBlankString, Item>()
    val matchedExistingItems = mutableSetOf<Item>()
    val recipeIngredients = ingredients.mapNotNull { input ->
        val itemId = when (val item = input.item) {
            is ItemInput.Selected -> item.itemId
            is ItemInput.Typed -> {
                val displayName = NotBlankString.of(item.text) ?: return@mapNotNull null
                val resolvedItem = existingItems[displayName]
                    ?.also { matchedExistingItems += it }
                    ?: newItems.getOrPut(displayName) { Item(ItemId.generate(), displayName) }
                resolvedItem.id
            }
        }
        RecipeIngredient(
            itemId = itemId,
            amount = input.amount,
        )
    }
    return ResolvedIngredients(
        recipeIngredients = recipeIngredients.toImmutableList(),
        newItems = newItems.values.toImmutableList(),
        matchedExistingItems = matchedExistingItems.toImmutableList(),
    )
}
