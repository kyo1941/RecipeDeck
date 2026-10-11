package com.example.recipe_deck.usecase.recipe

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
import com.example.recipe_deck.domain.recipe.RecipeIngredient
import com.example.recipe_deck.usecase.ingredient.IngredientInput
import com.example.recipe_deck.usecase.ingredient.ItemInput
import com.example.recipe_deck.usecase.item.linkToExistingItem
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
    val existingItemsById = existingItems.values.associateBy { it.id }
    val newItems = mutableMapOf<NotBlankString, Item>()
    val matchedExistingItems = mutableSetOf<Item>()
    val recipeIngredients = ingredients.mapNotNull { input ->
        val itemId = when (val item = input.item) {
            is ItemInput.Selected -> item.itemId
            is ItemInput.Typed -> {
                val displayName = NotBlankString.of(item.text) ?: return@mapNotNull null
                when (val linked = ItemReference.Unregistered(displayName).linkToExistingItem(existingItems)) {
                    is ItemReference.Registered -> {
                        existingItemsById[linked.itemId]?.let { matchedExistingItems += it }
                        linked.itemId
                    }
                    is ItemReference.Unregistered -> newItems
                        .getOrPut(linked.displayName) { Item(ItemId.generate(), linked.displayName) }
                        .id
                }
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
