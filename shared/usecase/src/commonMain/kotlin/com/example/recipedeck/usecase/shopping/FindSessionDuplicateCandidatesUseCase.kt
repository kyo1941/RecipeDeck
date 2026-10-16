package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.usecase.item.DuplicateCandidate
import com.example.recipedeck.usecase.item.DuplicateCandidateFinder
import com.example.recipedeck.usecase.item.DuplicateCandidateFinderFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class FindSessionDuplicateCandidatesUseCase(
    private val duplicateCandidateFinder: DuplicateCandidateFinder,
) {
    suspend operator fun invoke(displayName: String): FindSessionDuplicateCandidatesResult {
        val typedDisplayName = NotBlankString.of(displayName)
            ?: return FindSessionDuplicateCandidatesResult.Found(persistentListOf())

        return when (val found = duplicateCandidateFinder.findItemsAndSessionItems(typedDisplayName)) {
            is Outcome.Success -> FindSessionDuplicateCandidatesResult.Found(found.value)
            is Outcome.Failure -> found.error.toFindSessionDuplicateCandidatesResult()
        }
    }

    private fun DuplicateCandidateFinderFailure.toFindSessionDuplicateCandidatesResult(): FindSessionDuplicateCandidatesResult =
        when (this) {
            is DuplicateCandidateFinderFailure.Unexpected -> FindSessionDuplicateCandidatesResult.Unexpected(cause)
        }
}

sealed interface FindSessionDuplicateCandidatesResult {
    data class Found(val candidates: ImmutableList<DuplicateCandidate>) : FindSessionDuplicateCandidatesResult
    data class Unexpected(val cause: Throwable) : FindSessionDuplicateCandidatesResult
}
