package com.example.recipedeck.data.item

import com.example.recipedeck.data.InvalidStoredRecordException
import com.example.recipedeck.data.RecipeDeckDatabase
import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.coroutines.cancellation.CancellationException

class RoomItemRepository(database: RecipeDeckDatabase) : ItemRepository {
    private val dao = database.itemDao()

    override suspend fun save(item: Item): Outcome<Unit, ItemRepositoryFailure> =
        outcomeOf { dao.save(item.id.value, item.displayName.value) }

    override suspend fun findById(id: ItemId): Outcome<Item?, ItemRepositoryFailure> =
        outcomeOf { dao.findById(id.value)?.toItem() }

    override suspend fun findByIds(ids: Set<ItemId>): Outcome<ImmutableList<Item>, ItemRepositoryFailure> =
        outcomeOf { dao.findByIds(ids.map { it.value }).toItems() }

    override suspend fun findByDisplayName(displayName: NotBlankString): Outcome<Item?, ItemRepositoryFailure> =
        outcomeOf { dao.findByDisplayName(displayName.value)?.toItem() }

    override suspend fun findByDisplayNames(
        displayNames: Set<NotBlankString>,
    ): Outcome<ImmutableList<Item>, ItemRepositoryFailure> =
        outcomeOf { dao.findByDisplayNames(displayNames.map { it.value }).toItems() }

    override suspend fun search(keyword: NotBlankString): Outcome<ImmutableList<Item>, ItemRepositoryFailure> =
        outcomeOf { dao.search(keyword.value).toItems() }

    private suspend fun <T> outcomeOf(block: suspend () -> T): Outcome<T, ItemRepositoryFailure> =
        try {
            Outcome.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Outcome.Failure(ItemRepositoryFailure.Unexpected(e))
        }
}

private fun List<ItemEntity>.toItems(): ImmutableList<Item> = map { it.toItem() }.toImmutableList()

private fun ItemEntity.toItem(): Item = Item(
    id = ItemId(id),
    displayName = NotBlankString.of(displayName)
        ?: throw InvalidStoredRecordException("Item $id の表示名が空白だけになっている"),
)
