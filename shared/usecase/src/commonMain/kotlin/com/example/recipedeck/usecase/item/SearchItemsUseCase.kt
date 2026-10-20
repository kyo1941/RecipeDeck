package com.example.recipedeck.usecase.item

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class SearchItemsUseCase(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke(keyword: String): SearchItemsResult {
        val searchKeyword = NotBlankString.of(keyword) ?: return SearchItemsResult.Found(persistentListOf())

        return when (val found = itemRepository.search(searchKeyword)) {
            is Outcome.Success -> SearchItemsResult.Found(found.value)
            is Outcome.Failure -> found.error.toSearchItemsResult()
        }
    }

    private fun ItemRepositoryFailure.toSearchItemsResult(): SearchItemsResult = when (this) {
        is ItemRepositoryFailure.Unexpected -> SearchItemsResult.Unexpected(cause)
    }
}

sealed interface SearchItemsResult {
    data class Found(val items: ImmutableList<Item>) : SearchItemsResult
    data class Unexpected(val cause: Throwable) : SearchItemsResult
}
