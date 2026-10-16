package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class FindDuplicateCandidatesUseCase(
    private val duplicateCandidateFinder: DuplicateCandidateFinder,
) {
    suspend operator fun invoke(
        displayName: String,
        renamingItemId: ItemId? = null,
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
