package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.shopping.SessionItem
import kotlinx.collections.immutable.ImmutableList

interface DuplicateCandidateFinder {
    suspend fun findItems(
        displayName: NotBlankString,
        excludedItemId: ItemId?,
    ): Outcome<ImmutableList<Item>, DuplicateCandidateFinderFailure>

    suspend fun findItemsAndSessionItems(
        displayName: NotBlankString,
    ): Outcome<ImmutableList<DuplicateCandidate>, DuplicateCandidateFinderFailure>
}

sealed interface DuplicateCandidate {
    data class OfItem(val item: Item) : DuplicateCandidate
    data class OfSessionItem(val sessionItem: SessionItem) : DuplicateCandidate
}

sealed interface DuplicateCandidateFinderFailure {
    data class Unexpected(val cause: Throwable) : DuplicateCandidateFinderFailure
}
