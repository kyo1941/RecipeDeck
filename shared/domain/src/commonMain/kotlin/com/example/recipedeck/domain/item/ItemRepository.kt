package com.example.recipedeck.domain.item

import com.example.recipedeck.domain.Outcome
import kotlinx.collections.immutable.ImmutableList

interface ItemRepository {
    suspend fun save(item: Item): Outcome<Unit, ItemRepositoryFailure>
    suspend fun findAll(): Outcome<ImmutableList<Item>, ItemRepositoryFailure>
}
