package com.example.recipedeck.usecase.item

import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import com.example.recipedeck.usecase.notBlankStringOrFail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RenameItemUseCaseTest {
    private val itemRepository = FakeItemRepository()
    private val renameItem = RenameItemUseCase(itemRepository)

    private val kanjiOnion = Item(ItemId("item-kanji-onion"), notBlankStringOrFail("玉葱"))
    private val onion = Item(ItemId("item-onion"), notBlankStringOrFail("玉ねぎ"))

    @Test
    fun 指定された表示名にItemの表示名を変える() = runTest {
        itemRepository.savedItems += kanjiOnion

        val result = renameItem(kanjiOnion.id, "たまねぎ")

        val renamedItem = Item(kanjiOnion.id, notBlankStringOrFail("たまねぎ"))
        assertEquals(RenameItemResult.Renamed(renamedItem), result)
        assertEquals(listOf(renamedItem), itemRepository.savedItems)
    }

    @Test
    fun 表示名の前後の空白を除いて変える() = runTest {
        itemRepository.savedItems += kanjiOnion

        val result = renameItem(kanjiOnion.id, "　たまねぎ \n")

        val renamedItem = Item(kanjiOnion.id, notBlankStringOrFail("たまねぎ"))
        assertEquals(RenameItemResult.Renamed(renamedItem), result)
        assertEquals(listOf(renamedItem), itemRepository.savedItems)
    }

    @Test
    fun 表示名が空白だけなら変えずに失敗を返す() = runTest {
        itemRepository.savedItems += kanjiOnion

        val result = renameItem(kanjiOnion.id, " 　\n")

        assertEquals(RenameItemResult.DisplayNameRequired, result)
        assertEquals(listOf(kanjiOnion), itemRepository.savedItems)
    }

    @Test
    fun 前後の空白を除いた表示名が同じ別のItemがあれば変えずにそのItemを返す() = runTest {
        itemRepository.savedItems += listOf(kanjiOnion, onion)

        val result = renameItem(kanjiOnion.id, " 玉ねぎ　")

        assertEquals(RenameItemResult.AlreadyExists(onion), result)
        assertEquals(listOf(kanjiOnion, onion), itemRepository.savedItems)
    }

    @Test
    fun 今と同じ表示名なら変えたとして返す() = runTest {
        itemRepository.savedItems += kanjiOnion

        val result = renameItem(kanjiOnion.id, " 玉葱　")

        assertEquals(RenameItemResult.Renamed(kanjiOnion), result)
        assertEquals(listOf(kanjiOnion), itemRepository.savedItems)
    }

    @Test
    fun Itemがもうなければ何も保存せずにそのことを返す() = runTest {
        itemRepository.savedItems += onion

        val result = renameItem(kanjiOnion.id, "たまねぎ")

        assertEquals(RenameItemResult.ItemNoLongerExists, result)
        assertEquals(listOf(onion), itemRepository.savedItems)
    }

    @Test
    fun 読み込みに失敗したら保存せずに原因を持った予期しない失敗を返す() = runTest {
        itemRepository.savedItems += kanjiOnion
        val cause = IllegalStateException("保存先から読み込めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = renameItem(kanjiOnion.id, "たまねぎ")

        assertEquals(RenameItemResult.Unexpected(cause), result)
        assertEquals(listOf(kanjiOnion), itemRepository.savedItems)
    }

    @Test
    fun 保存に失敗したら原因を持った予期しない失敗を返す() = runTest {
        itemRepository.savedItems += kanjiOnion
        val cause = IllegalStateException("保存先に書き込めない")
        itemRepository.saveFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = renameItem(kanjiOnion.id, "たまねぎ")

        assertEquals(RenameItemResult.Unexpected(cause), result)
    }
}
