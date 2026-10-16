package com.example.recipe_deck.usecase.item

import com.example.recipe_deck.domain.item.Item
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.usecase.notBlankStringOrFail
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FindDuplicateCandidatesUseCaseTest {
    private val duplicateCandidateFinder = FakeDuplicateCandidateFinder()
    private val findDuplicateCandidates = FindDuplicateCandidatesUseCase(duplicateCandidateFinder)

    @Test
    fun 前後の空白を除いた表示名で探し候補のItemを返す() = runTest {
        val onion = Item(ItemId("item-onion"), notBlankStringOrFail("たまねぎ"))
        duplicateCandidateFinder.itemCandidates = persistentListOf(onion)

        val result = findDuplicateCandidates(" 玉葱　")

        assertEquals(FindDuplicateCandidatesResult.Found(persistentListOf(onion)), result)
        assertEquals(listOf(notBlankStringOrFail("玉葱")), duplicateCandidateFinder.requestedDisplayNames)
    }

    @Test
    fun 名前を変えるItemが指定されなければ何も除かずに探す() = runTest {
        findDuplicateCandidates("玉葱")

        assertEquals(listOf<ItemId?>(null), duplicateCandidateFinder.requestedExcludedItemIds)
    }

    @Test
    fun 名前を変えるItemが指定されたらそのItemを除いて探す() = runTest {
        val renamingItemId = ItemId("item-kanji-onion")

        findDuplicateCandidates("玉ねぎ", renamingItemId)

        assertEquals(listOf<ItemId?>(renamingItemId), duplicateCandidateFinder.requestedExcludedItemIds)
    }

    @Test
    fun 入力が空白だけなら探さずに何も返さない() = runTest {
        val result = findDuplicateCandidates(" 　\n")

        assertEquals(FindDuplicateCandidatesResult.Found(persistentListOf()), result)
        assertEquals(emptyList(), duplicateCandidateFinder.requestedDisplayNames)
    }

    @Test
    fun 探すのに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("候補を探せない")
        duplicateCandidateFinder.findFailure = DuplicateCandidateFinderFailure.Unexpected(cause)

        val result = findDuplicateCandidates("玉葱")

        assertEquals(FindDuplicateCandidatesResult.Unexpected(cause), result)
    }
}
