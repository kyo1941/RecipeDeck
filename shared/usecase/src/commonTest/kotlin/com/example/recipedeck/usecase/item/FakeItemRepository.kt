package com.example.recipedeck.usecase.item

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
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

    override suspend fun findAll(): Outcome<ImmutableList<Item>, ItemRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(savedItems.toImmutableList())
    }
}
