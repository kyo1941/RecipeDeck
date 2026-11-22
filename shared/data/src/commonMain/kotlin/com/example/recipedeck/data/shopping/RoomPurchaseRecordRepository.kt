package com.example.recipedeck.data.shopping

import androidx.room3.withWriteTransaction
import com.example.recipedeck.data.RecipeDeckDatabase
import com.example.recipedeck.data.observeOutcomeOf
import com.example.recipedeck.data.outcomeOf
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.shopping.PurchaseRecordRepository
import com.example.recipedeck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipedeck.domain.shopping.ShoppingListItemSourceId
import com.example.recipedeck.domain.shopping.ShoppingSessionId
import kotlinx.coroutines.flow.Flow

class RoomPurchaseRecordRepository(private val database: RecipeDeckDatabase) : PurchaseRecordRepository {
    private val dao = database.purchaseRecordDao()

    override suspend fun findPurchasedSourceIds(
        sessionId: ShoppingSessionId,
    ): Outcome<Set<ShoppingListItemSourceId>, PurchaseRecordRepositoryFailure> =
        outcomeOf(PurchaseRecordRepositoryFailure::Unexpected) { readPurchasedSourceIds(sessionId) }

    override fun observePurchasedSourceIds(
        sessionId: ShoppingSessionId,
    ): Flow<Outcome<Set<ShoppingListItemSourceId>, PurchaseRecordRepositoryFailure>> =
        database.observeOutcomeOf(PurchaseRecordEntity.TABLE_NAME, toFailure = PurchaseRecordRepositoryFailure::Unexpected) {
            readPurchasedSourceIds(sessionId)
        }

    override suspend fun record(
        sessionId: ShoppingSessionId,
        sourceIds: Set<ShoppingListItemSourceId>,
        isPurchased: Boolean,
    ): Outcome<Unit, PurchaseRecordRepositoryFailure> =
        outcomeOf(PurchaseRecordRepositoryFailure::Unexpected) {
            database.withWriteTransaction {
                if (isPurchased) {
                    sourceIds.forEach { dao.markPurchased(sessionId.value, it.value) }
                } else {
                    dao.markUnpurchased(sessionId.value, sourceIds.map { it.value })
                }
            }
        }

    private suspend fun readPurchasedSourceIds(sessionId: ShoppingSessionId): Set<ShoppingListItemSourceId> =
        dao.findPurchasedSourceIds(sessionId.value).map(::ShoppingListItemSourceId).toSet()
}
