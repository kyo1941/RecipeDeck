package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.Outcome

interface PurchaseRecordRepository {
    suspend fun findPurchasedSourceIds(sessionId: ShoppingSessionId): Outcome<Set<ShoppingListItemSourceId>, PurchaseRecordRepositoryFailure>
    suspend fun mark(
        sessionId: ShoppingSessionId,
        sourceIds: List<ShoppingListItemSourceId>,
        isPurchased: Boolean,
    ): Outcome<Unit, PurchaseRecordRepositoryFailure>
}
