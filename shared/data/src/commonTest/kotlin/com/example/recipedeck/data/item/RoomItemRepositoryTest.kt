package com.example.recipedeck.data.item

import com.example.recipedeck.data.notBlankStringOrFail
import com.example.recipedeck.data.runTestWithRecipeDeckDatabase
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RoomItemRepositoryTest {
    private fun item(id: String, displayName: String) = Item(ItemId(id), notBlankStringOrFail(displayName))

    @Test
    fun 保存したItemをIDで読める() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)

        repository.save(item("item-1", "玉ねぎ"))

        assertEquals(Outcome.Success(item("item-1", "玉ねぎ")), repository.findById(ItemId("item-1")))
    }

    @Test
    fun 無いIDを読むとnullを返す() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)

        assertEquals(Outcome.Success(null), repository.findById(ItemId("item-1")))
    }

    @Test
    fun 同じIDで保存すると上書きする() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "玉ねぎ"))

        val result = repository.save(item("item-1", "たまねぎ"))

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(Outcome.Success(item("item-1", "たまねぎ")), repository.findById(ItemId("item-1")))
    }

    @Test
    fun 別のItemと同じ表示名で作ると失敗し元のItemは残る() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "玉ねぎ"))

        val result = repository.save(item("item-2", "玉ねぎ"))

        assertIs<Outcome.Failure<ItemRepositoryFailure.Unexpected>>(result)
        assertEquals(Outcome.Success(item("item-1", "玉ねぎ")), repository.findById(ItemId("item-1")))
        assertEquals(Outcome.Success(null), repository.findById(ItemId("item-2")))
    }

    @Test
    fun 別のItemと同じ表示名にrenameすると失敗し両方とも変わらない() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "玉ねぎ"))
        repository.save(item("item-2", "長ネギ"))

        val result = repository.save(item("item-2", "玉ねぎ"))

        assertIs<Outcome.Failure<ItemRepositoryFailure.Unexpected>>(result)
        assertEquals(Outcome.Success(item("item-1", "玉ねぎ")), repository.findById(ItemId("item-1")))
        assertEquals(Outcome.Success(item("item-2", "長ネギ")), repository.findById(ItemId("item-2")))
    }

    @Test
    fun 指定したIDのItemだけを読み無いIDは含めない() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "玉ねぎ"))
        repository.save(item("item-2", "長ネギ"))
        repository.save(item("item-3", "豆腐"))

        val result = repository.findByIds(setOf(ItemId("item-1"), ItemId("item-3"), ItemId("item-9")))

        assertEquals(Outcome.Success(persistentListOf(item("item-1", "玉ねぎ"), item("item-3", "豆腐"))), result)
    }

    @Test
    fun 表示名は完全一致したItemだけを読む() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "玉ねぎ"))
        repository.save(item("item-2", "新玉ねぎ"))

        assertEquals(Outcome.Success(item("item-1", "玉ねぎ")), repository.findByDisplayName(notBlankStringOrFail("玉ねぎ")))
        assertEquals(Outcome.Success(null), repository.findByDisplayName(notBlankStringOrFail("ねぎ")))
    }

    @Test
    fun 複数の表示名は完全一致したItemだけを読む() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "玉ねぎ"))
        repository.save(item("item-2", "新玉ねぎ"))
        repository.save(item("item-3", "豆腐"))

        val result = repository.findByDisplayNames(setOf(notBlankStringOrFail("玉ねぎ"), notBlankStringOrFail("豆腐"), notBlankStringOrFail("ねぎ")))

        assertEquals(Outcome.Success(persistentListOf(item("item-1", "玉ねぎ"), item("item-3", "豆腐"))), result)
    }

    @Test
    fun 検索は表示名の部分一致で探す() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "長ネギ"))
        repository.save(item("item-2", "豆腐"))

        assertEquals(Outcome.Success(persistentListOf(item("item-1", "長ネギ"))), repository.search(notBlankStringOrFail("ネギ")))
    }

    @Test
    fun 検索は完全一致と前方一致と部分一致の順に並べる() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "玉ねぎ"))
        repository.save(item("item-2", "ねぎ塩"))
        repository.save(item("item-3", "ねぎ"))

        val result = repository.search(notBlankStringOrFail("ねぎ"))

        assertEquals(
            Outcome.Success(persistentListOf(item("item-3", "ねぎ"), item("item-2", "ねぎ塩"), item("item-1", "玉ねぎ"))),
            result,
        )
    }

    @Test
    fun 検索は同じ一致の度合いなら表示名の短い順に並べ同じ長さなら作った順に並べる() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "新玉ねぎ"))
        repository.save(item("item-2", "長ねぎ"))
        repository.save(item("item-3", "玉ねぎ"))

        val result = repository.search(notBlankStringOrFail("ねぎ"))

        assertEquals(
            Outcome.Success(persistentListOf(item("item-2", "長ねぎ"), item("item-3", "玉ねぎ"), item("item-1", "新玉ねぎ"))),
            result,
        )
    }

    @Test
    fun 検索は英字の大文字と小文字を区別する() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "Tabasco"))

        assertEquals(Outcome.Success(persistentListOf()), repository.search(notBlankStringOrFail("tabasco")))
    }

    @Test
    fun 検索はパーセントとアンダースコアを文字として扱う() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomItemRepository(database)
        repository.save(item("item-1", "果汁100%ジュース"))
        repository.save(item("item-2", "果汁100ジュース"))
        repository.save(item("item-3", "A_B"))
        repository.save(item("item-4", "AxB"))

        assertEquals(Outcome.Success(persistentListOf(item("item-1", "果汁100%ジュース"))), repository.search(notBlankStringOrFail("0%")))
        assertEquals(Outcome.Success(persistentListOf(item("item-3", "A_B"))), repository.search(notBlankStringOrFail("A_B")))
    }

    @Test
    fun 読み戻した表示名が空白だけなら失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        database.itemDao().save("item-1", " ")
        val repository = RoomItemRepository(database)

        assertIs<Outcome.Failure<ItemRepositoryFailure.Unexpected>>(repository.findById(ItemId("item-1")))
    }
}
