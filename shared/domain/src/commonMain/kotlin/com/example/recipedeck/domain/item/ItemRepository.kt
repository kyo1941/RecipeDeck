package com.example.recipedeck.domain.item

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import kotlinx.collections.immutable.ImmutableList

interface ItemRepository {
    suspend fun save(item: Item): Outcome<Unit, ItemRepositoryFailure>
    suspend fun findById(id: ItemId): Outcome<Item?, ItemRepositoryFailure>
    suspend fun findByIds(ids: Set<ItemId>): Outcome<ImmutableList<Item>, ItemRepositoryFailure>
    suspend fun findByDisplayName(displayName: NotBlankString): Outcome<Item?, ItemRepositoryFailure>
    suspend fun findByDisplayNames(displayNames: Set<NotBlankString>): Outcome<ImmutableList<Item>, ItemRepositoryFailure>
    suspend fun search(keyword: NotBlankString): Outcome<ImmutableList<Item>, ItemRepositoryFailure>
}
