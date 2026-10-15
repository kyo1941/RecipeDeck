package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.shopping.PurchaseRecordRepository
import com.example.recipe_deck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipe_deck.domain.shopping.ShoppingListItem
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepository
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepositoryFailure
import kotlinx.collections.immutable.ImmutableList

class ShowShoppingListUseCase(
    private val shoppingSessionRepository: ShoppingSessionRepository,
    private val purchaseRecordRepository: PurchaseRecordRepository,
) {
    suspend operator fun invoke(): ShowShoppingListResult {
        val session = when (val found = shoppingSessionRepository.findActive()) {
            is Outcome.Success -> found.value ?: return ShowShoppingListResult.NoActiveSession
            is Outcome.Failure -> return found.error.toShowShoppingListResult()
        }
        return when (val found = purchaseRecordRepository.findPurchasedSourceIds(session.id)) {
            is Outcome.Success -> ShowShoppingListResult.Shown(session.toShoppingList(found.value))
            is Outcome.Failure -> found.error.toShowShoppingListResult()
        }
    }

    private fun ShoppingSessionRepositoryFailure.toShowShoppingListResult(): ShowShoppingListResult = when (this) {
        is ShoppingSessionRepositoryFailure.Unexpected -> ShowShoppingListResult.Unexpected(cause)
    }

    private fun PurchaseRecordRepositoryFailure.toShowShoppingListResult(): ShowShoppingListResult = when (this) {
        is PurchaseRecordRepositoryFailure.Unexpected -> ShowShoppingListResult.Unexpected(cause)
    }
}

sealed interface ShowShoppingListResult {
    data class Shown(val items: ImmutableList<ShoppingListItem>) : ShowShoppingListResult
    data object NoActiveSession : ShowShoppingListResult
    data class Unexpected(val cause: Throwable) : ShowShoppingListResult
}
