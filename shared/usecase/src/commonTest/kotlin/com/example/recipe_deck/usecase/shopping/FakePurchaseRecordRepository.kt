package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.shopping.PurchaseRecordRepository
import com.example.recipe_deck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipe_deck.domain.shopping.ShoppingListItemSourceId
import com.example.recipe_deck.domain.shopping.ShoppingSessionId

class FakePurchaseRecordRepository : PurchaseRecordRepository {
    val purchasedSourceIds = mutableMapOf<ShoppingSessionId, Set<ShoppingListItemSourceId>>()
    var findFailure: PurchaseRecordRepositoryFailure? = null
    var markFailure: PurchaseRecordRepositoryFailure? = null

    override suspend fun findPurchasedSourceIds(
        sessionId: ShoppingSessionId,
    ): Outcome<Set<ShoppingListItemSourceId>, PurchaseRecordRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(purchasedSourceIds[sessionId].orEmpty())
    }

    override suspend fun mark(
        sessionId: ShoppingSessionId,
        sourceIds: List<ShoppingListItemSourceId>,
        isPurchased: Boolean,
    ): Outcome<Unit, PurchaseRecordRepositoryFailure> {
        markFailure?.let { return Outcome.Failure(it) }
        val current = purchasedSourceIds[sessionId].orEmpty()
        purchasedSourceIds[sessionId] = if (isPurchased) current + sourceIds else current - sourceIds.toSet()
        return Outcome.Success(Unit)
    }
}
