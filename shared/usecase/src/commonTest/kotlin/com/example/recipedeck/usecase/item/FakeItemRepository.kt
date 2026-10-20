package com.example.recipedeck.usecase.item

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class FakeItemRepository : ItemRepository {
    val savedItems = mutableListOf<Item>()
    var saveFailure: ItemRepositoryFailure? = null
    var findFailure: ItemRepositoryFailure? = null

    override suspend fun save(item: Item): Outcome<Unit, ItemRepositoryFailure> {
        saveFailure?.let { return Outcome.Failure(it) }
        val index = savedItems.indexOfFirst { it.id == item.id }
        if (index >= 0) savedItems[index] = item else savedItems += item
        return Outcome.Success(Unit)
    }

    override suspend fun findById(id: ItemId): Outcome<Item?, ItemRepositoryFailure> =
        find { items -> items.firstOrNull { it.id == id } }

    override suspend fun findByIds(ids: Set<ItemId>): Outcome<ImmutableList<Item>, ItemRepositoryFailure> =
        find { items -> items.filter { it.id in ids }.toImmutableList() }

    override suspend fun findByDisplayName(displayName: NotBlankString): Outcome<Item?, ItemRepositoryFailure> =
        find { items -> items.firstOrNull { it.displayName == displayName } }

    override suspend fun findByDisplayNames(
        displayNames: Set<NotBlankString>,
    ): Outcome<ImmutableList<Item>, ItemRepositoryFailure> =
        find { items -> items.filter { it.displayName in displayNames }.toImmutableList() }

    override suspend fun search(keyword: NotBlankString): Outcome<ImmutableList<Item>, ItemRepositoryFailure> =
        find { items ->
            items
                .filter { it.displayName.value.contains(keyword.value) }
                .sortedWith(
                    compareBy(
                        { if (it.displayName == keyword) 0 else if (it.displayName.value.startsWith(keyword.value)) 1 else 2 },
                        { it.displayName.value.length },
                    ),
                )
                .toImmutableList()
        }

    private fun <T> find(select: (List<Item>) -> T): Outcome<T, ItemRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(select(savedItems))
    }
}
