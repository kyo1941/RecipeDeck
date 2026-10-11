package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemReference

internal fun ItemReference.Unregistered.linkToExistingItem(existingItems: Map<NotBlankString, Item>): ItemReference {
    val existingItem = existingItems[displayName] ?: return this
    return ItemReference.Registered(existingItem.id)
}
