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
) {
    val isPurchased: Boolean
        get() = sourcesToBuy.all { it.isPurchased }

    val sourceIdsToBuy: List<ShoppingListItemSourceId>
        get() = sourcesToBuy.map { it.id }

    private val sourcesToBuy: List<ShoppingListItemSource>
        get() = sources.filterNot { it is ShoppingListItemSource.FromRecipe && it.isExcluded }
}

sealed interface ShoppingListItemSource {
    val id: ShoppingListItemSourceId
    val isPurchased: Boolean

    data class FromRecipe(
        override val id: ShoppingListItemSourceId,
        val recipeName: NotBlankString,
        val multiplier: Multiplier,
        val amount: Amount?,
        val isExcluded: Boolean,
        override val isPurchased: Boolean,
    ) : ShoppingListItemSource

    data class Manual(
        override val id: ShoppingListItemSourceId,
        val amount: Amount?,
        override val isPurchased: Boolean,
    ) : ShoppingListItemSource
}
