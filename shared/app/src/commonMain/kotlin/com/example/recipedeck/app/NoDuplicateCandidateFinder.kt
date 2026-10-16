package com.example.recipedeck.app

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.usecase.item.DuplicateCandidate
import com.example.recipedeck.usecase.item.DuplicateCandidateFinder
import com.example.recipedeck.usecase.item.DuplicateCandidateFinderFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal object NoDuplicateCandidateFinder : DuplicateCandidateFinder {
    override suspend fun findItems(
        displayName: NotBlankString,
        excludedItemId: ItemId?,
    ): Outcome<ImmutableList<Item>, DuplicateCandidateFinderFailure> = Outcome.Success(persistentListOf())

    override suspend fun findItemsAndSessionItems(
        displayName: NotBlankString,
    ): Outcome<ImmutableList<DuplicateCandidate>, DuplicateCandidateFinderFailure> = Outcome.Success(persistentListOf())
}
