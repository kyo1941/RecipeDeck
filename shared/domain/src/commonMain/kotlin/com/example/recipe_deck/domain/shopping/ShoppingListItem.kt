package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.Multiplier
import com.example.recipe_deck.domain.item.ItemId
import kotlinx.collections.immutable.ImmutableList

data class ShoppingListItem(
    val sessionItemId: SessionItemId,
    val displayName: NotBlankString,
    val itemId: ItemId?,
    val sources: ImmutableList<ShoppingListItemSource>,
)

sealed interface ShoppingListItemSource {
    val id: ShoppingListItemSourceId

    data class FromRecipe(
        override val id: ShoppingListItemSourceId,
        val recipeName: NotBlankString,
        val multiplier: Multiplier,
        val amount: Amount?,
        val isExcluded: Boolean,
    ) : ShoppingListItemSource

    data class Manual(
        override val id: ShoppingListItemSourceId,
        val amount: Amount?,
    ) : ShoppingListItemSource
}
