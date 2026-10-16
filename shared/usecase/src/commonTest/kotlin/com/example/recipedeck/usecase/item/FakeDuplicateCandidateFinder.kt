package com.example.recipedeck.usecase.item

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class FakeDuplicateCandidateFinder : DuplicateCandidateFinder {
    var itemCandidates: ImmutableList<Item> = persistentListOf()
    var candidates: ImmutableList<DuplicateCandidate> = persistentListOf()
    var findFailure: DuplicateCandidateFinderFailure? = null
    val requestedDisplayNames = mutableListOf<NotBlankString>()
    val requestedExcludedItemIds = mutableListOf<ItemId?>()

    override suspend fun findItems(
        displayName: NotBlankString,
        excludedItemId: ItemId?,
    ): Outcome<ImmutableList<Item>, DuplicateCandidateFinderFailure> {
        requestedDisplayNames += displayName
        requestedExcludedItemIds += excludedItemId
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(itemCandidates)
    }

    override suspend fun findItemsAndSessionItems(
        displayName: NotBlankString,
    ): Outcome<ImmutableList<DuplicateCandidate>, DuplicateCandidateFinderFailure> {
        requestedDisplayNames += displayName
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(candidates)
    }
}
