package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.Outcome

interface PurchaseRecordRepository {
    suspend fun findPurchasedSourceIds(sessionId: ShoppingSessionId): Outcome<Set<ShoppingListItemSourceId>, PurchaseRecordRepositoryFailure>
    suspend fun record(
        sessionId: ShoppingSessionId,
        sourceIds: Set<ShoppingListItemSourceId>,
        isPurchased: Boolean,
    ): Outcome<Unit, PurchaseRecordRepositoryFailure>
}
