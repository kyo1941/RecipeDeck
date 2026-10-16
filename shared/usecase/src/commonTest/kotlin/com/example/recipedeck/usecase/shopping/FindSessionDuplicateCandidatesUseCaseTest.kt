package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.shopping.SessionItem
import com.example.recipedeck.domain.shopping.SessionItemId
import com.example.recipedeck.usecase.item.DuplicateCandidate
import com.example.recipedeck.usecase.item.DuplicateCandidateFinderFailure
import com.example.recipedeck.usecase.item.FakeDuplicateCandidateFinder
import com.example.recipedeck.usecase.notBlankStringOrFail
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FindSessionDuplicateCandidatesUseCaseTest {
    private val duplicateCandidateFinder = FakeDuplicateCandidateFinder()
    private val findSessionDuplicateCandidates = FindSessionDuplicateCandidatesUseCase(duplicateCandidateFinder)

    @Test
    fun 前後の空白を除いた表示名で探しItemとSessionの品目の候補を返す() = runTest {
        val candidates = persistentListOf(
            DuplicateCandidate.OfItem(Item(ItemId("item-onion"), notBlankStringOrFail("たまねぎ"))),
            DuplicateCandidate.OfSessionItem(
                SessionItem(SessionItemId("session-item-onion"), notBlankStringOrFail("玉ねぎ"), itemId = null, manualAddition = null),
            ),
        )
        duplicateCandidateFinder.candidates = candidates

        val result = findSessionDuplicateCandidates(" 玉葱　")

        assertEquals(FindSessionDuplicateCandidatesResult.Found(candidates), result)
        assertEquals(listOf(notBlankStringOrFail("玉葱")), duplicateCandidateFinder.requestedDisplayNames)
    }

    @Test
    fun 入力が空白だけなら探さずに何も返さない() = runTest {
        val result = findSessionDuplicateCandidates(" 　\n")

        assertEquals(FindSessionDuplicateCandidatesResult.Found(persistentListOf()), result)
        assertEquals(emptyList(), duplicateCandidateFinder.requestedDisplayNames)
    }

    @Test
    fun 探すのに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("候補を探せない")
        duplicateCandidateFinder.findFailure = DuplicateCandidateFinderFailure.Unexpected(cause)

        val result = findSessionDuplicateCandidates("玉葱")

        assertEquals(FindSessionDuplicateCandidatesResult.Unexpected(cause), result)
    }
}
