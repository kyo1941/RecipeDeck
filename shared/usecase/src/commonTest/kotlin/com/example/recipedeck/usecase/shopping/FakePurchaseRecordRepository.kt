package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.shopping.PurchaseRecordRepository
import com.example.recipedeck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipedeck.domain.shopping.ShoppingListItemSourceId
import com.example.recipedeck.domain.shopping.ShoppingSessionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakePurchaseRecordRepository : PurchaseRecordRepository {
    val purchasedSourceIds = mutableMapOf<ShoppingSessionId, Set<ShoppingListItemSourceId>>()
    var findFailure: PurchaseRecordRepositoryFailure? = null
    var recordFailure: PurchaseRecordRepositoryFailure? = null
    private val recordCount = MutableStateFlow(0)

    override suspend fun findPurchasedSourceIds(
        sessionId: ShoppingSessionId,
    ): Outcome<Set<ShoppingListItemSourceId>, PurchaseRecordRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(purchasedSourceIds[sessionId].orEmpty())
    }

    override fun observePurchasedSourceIds(
        sessionId: ShoppingSessionId,
    ): Flow<Outcome<Set<ShoppingListItemSourceId>, PurchaseRecordRepositoryFailure>> =
        recordCount.map { findPurchasedSourceIds(sessionId) }

    override suspend fun record(
        sessionId: ShoppingSessionId,
        sourceIds: Set<ShoppingListItemSourceId>,
        isPurchased: Boolean,
    ): Outcome<Unit, PurchaseRecordRepositoryFailure> {
        recordFailure?.let { return Outcome.Failure(it) }
        val current = purchasedSourceIds[sessionId].orEmpty()
        purchasedSourceIds[sessionId] = if (isPurchased) current + sourceIds else current - sourceIds
        recordCount.update { it + 1 }
        return Outcome.Success(Unit)
    }
}
