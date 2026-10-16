package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemRepository
import com.example.recipe_deck.domain.item.ItemRepositoryFailure

class RenameItemUseCase(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke(itemId: ItemId, displayName: String): RenameItemResult {
        val newDisplayName = NotBlankString.of(displayName) ?: return RenameItemResult.DisplayNameRequired

        val existingItems = when (val found = itemRepository.findAll()) {
            is Outcome.Success -> found.value
            is Outcome.Failure -> return found.error.toRenameItemResult()
        }
        val item = existingItems.firstOrNull { it.id == itemId } ?: return RenameItemResult.ItemNoLongerExists
        existingItems.firstOrNull { it.id != itemId && it.displayName == newDisplayName }?.let {
            return RenameItemResult.AlreadyExists(it)
        }

        val renamedItem = item.copy(displayName = newDisplayName)
        return when (val saved = itemRepository.save(renamedItem)) {
            is Outcome.Success -> RenameItemResult.Renamed(renamedItem)
            is Outcome.Failure -> saved.error.toRenameItemResult()
        }
    }

    private fun ItemRepositoryFailure.toRenameItemResult(): RenameItemResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> RenameItemResult.Unexpected(cause)
    }
}

sealed interface RenameItemResult {
    data class Renamed(val renamedItem: Item) : RenameItemResult
    data object DisplayNameRequired : RenameItemResult
    data class AlreadyExists(val existingItem: Item) : RenameItemResult
    data object ItemNoLongerExists : RenameItemResult
    data class Unexpected(val cause: Throwable) : RenameItemResult
}
