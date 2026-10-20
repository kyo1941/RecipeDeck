package com.example.recipedeck.usecase.item

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure

class RenameItemUseCase(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke(itemId: ItemId, displayName: String): RenameItemResult {
        val newDisplayName = NotBlankString.of(displayName) ?: return RenameItemResult.DisplayNameRequired

        val item = when (val found = itemRepository.findById(itemId)) {
            is Outcome.Success -> found.value ?: return RenameItemResult.ItemNoLongerExists
            is Outcome.Failure -> return found.error.toRenameItemResult()
        }
        when (val found = itemRepository.findByDisplayName(newDisplayName)) {
            is Outcome.Success -> found.value?.takeIf { it.id != itemId }?.let {
                return RenameItemResult.AlreadyExists(it)
            }
            is Outcome.Failure -> return found.error.toRenameItemResult()
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
