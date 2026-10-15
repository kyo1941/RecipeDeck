package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.shopping.PurchaseRecordRepository
import com.example.recipe_deck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipe_deck.domain.shopping.SessionItemId
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepository
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepositoryFailure

class MarkShoppingListItemPurchasedUseCase(
    private val shoppingSessionRepository: ShoppingSessionRepository,
    private val purchaseRecordRepository: PurchaseRecordRepository,
) {
    suspend operator fun invoke(sessionItemId: SessionItemId, isPurchased: Boolean): MarkShoppingListItemPurchasedResult {
        val session = when (val found = shoppingSessionRepository.findActive()) {
            is Outcome.Success -> found.value ?: return MarkShoppingListItemPurchasedResult.NoActiveSession
            is Outcome.Failure -> return found.error.toMarkShoppingListItemPurchasedResult()
        }
        val purchasedSourceIds = when (val found = purchaseRecordRepository.findPurchasedSourceIds(session.id)) {
            is Outcome.Success -> found.value
            is Outcome.Failure -> return found.error.toMarkShoppingListItemPurchasedResult()
        }
        val item = session.toShoppingList(purchasedSourceIds).find { it.sessionItemId == sessionItemId }
            ?: return MarkShoppingListItemPurchasedResult.ShoppingListItemNoLongerExists
        val sourceIds = item.sourcesToBuy.map { it.id }
        if (sourceIds.isEmpty()) return MarkShoppingListItemPurchasedResult.AllSourcesExcluded

        return when (val marked = purchaseRecordRepository.mark(session.id, sourceIds, isPurchased)) {
            is Outcome.Success -> MarkShoppingListItemPurchasedResult.Marked
            is Outcome.Failure -> marked.error.toMarkShoppingListItemPurchasedResult()
        }
    }

    private fun ShoppingSessionRepositoryFailure.toMarkShoppingListItemPurchasedResult(): MarkShoppingListItemPurchasedResult =
        when (this) {
            is ShoppingSessionRepositoryFailure.Unexpected -> MarkShoppingListItemPurchasedResult.Unexpected(cause)
        }

    private fun PurchaseRecordRepositoryFailure.toMarkShoppingListItemPurchasedResult(): MarkShoppingListItemPurchasedResult =
        when (this) {
            is PurchaseRecordRepositoryFailure.Unexpected -> MarkShoppingListItemPurchasedResult.Unexpected(cause)
        }
}

sealed interface MarkShoppingListItemPurchasedResult {
    data object Marked : MarkShoppingListItemPurchasedResult
    data object NoActiveSession : MarkShoppingListItemPurchasedResult
    data object ShoppingListItemNoLongerExists : MarkShoppingListItemPurchasedResult
    data object AllSourcesExcluded : MarkShoppingListItemPurchasedResult
    data class Unexpected(val cause: Throwable) : MarkShoppingListItemPurchasedResult
}
