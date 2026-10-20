package com.example.recipedeck.usecase.item

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure

class CreateItemUseCase(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke(displayName: String): CreateItemResult {
        val itemDisplayName = NotBlankString.of(displayName) ?: return CreateItemResult.DisplayNameRequired

        when (val found = itemRepository.findByDisplayName(itemDisplayName)) {
            is Outcome.Success -> found.value?.let { return CreateItemResult.AlreadyExists(it) }
            is Outcome.Failure -> return found.error.toCreateItemResult()
        }

        val item = Item(
            id = ItemId.generate(),
            displayName = itemDisplayName,
        )
        return when (val saved = itemRepository.save(item)) {
            is Outcome.Success -> CreateItemResult.Created(item.id)
            is Outcome.Failure -> saved.error.toCreateItemResult()
        }
    }

    private fun ItemRepositoryFailure.toCreateItemResult(): CreateItemResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> CreateItemResult.Unexpected(cause)
    }
}

sealed interface CreateItemResult {
    data class Created(val itemId: ItemId) : CreateItemResult
    data object DisplayNameRequired : CreateItemResult
    data class AlreadyExists(val existingItem: Item) : CreateItemResult
    data class Unexpected(val cause: Throwable) : CreateItemResult
}
