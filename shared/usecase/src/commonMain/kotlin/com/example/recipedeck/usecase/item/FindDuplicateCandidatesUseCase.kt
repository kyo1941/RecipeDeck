package com.example.recipedeck.usecase.item

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

fun interface FindDuplicateCandidatesUseCase {
    suspend operator fun invoke(
        displayName: String,
        renamingItemId: ItemId?,
    ): FindDuplicateCandidatesResult
}

fun FindDuplicateCandidatesUseCase(
    duplicateCandidateFinder: DuplicateCandidateFinder,
): FindDuplicateCandidatesUseCase = object : FindDuplicateCandidatesUseCase {
    override suspend fun invoke(
        displayName: String,
        renamingItemId: ItemId?,
    ): FindDuplicateCandidatesResult {
        val typedDisplayName = NotBlankString.of(displayName)
            ?: return FindDuplicateCandidatesResult.Found(persistentListOf())

        return when (val found = duplicateCandidateFinder.findItems(typedDisplayName, excludedItemId = renamingItemId)) {
            is Outcome.Success -> FindDuplicateCandidatesResult.Found(found.value)
            is Outcome.Failure -> found.error.toFindDuplicateCandidatesResult()
        }
    }

    private fun DuplicateCandidateFinderFailure.toFindDuplicateCandidatesResult(): FindDuplicateCandidatesResult =
        when (this) {
            is DuplicateCandidateFinderFailure.Unexpected -> FindDuplicateCandidatesResult.Unexpected(cause)
        }
}

sealed interface FindDuplicateCandidatesResult {
    data class Found(val items: ImmutableList<Item>) : FindDuplicateCandidatesResult
    data class Unexpected(val cause: Throwable) : FindDuplicateCandidatesResult
}
