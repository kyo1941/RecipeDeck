package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.domain.Outcome
import com.example.recipe_deck.domain.shopping.ShoppingListItem
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepository
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepositoryFailure
import kotlinx.collections.immutable.ImmutableList

class ShowShoppingListUseCase(
    private val shoppingSessionRepository: ShoppingSessionRepository,
) {
    suspend operator fun invoke(): ShowShoppingListResult = when (val found = shoppingSessionRepository.findActive()) {
        is Outcome.Success -> found.value
            ?.let { ShowShoppingListResult.Shown(it.toShoppingList()) }
            ?: ShowShoppingListResult.NoActiveSession
        is Outcome.Failure -> found.error.toShowShoppingListResult()
    }

    private fun ShoppingSessionRepositoryFailure.toShowShoppingListResult(): ShowShoppingListResult = when (this) {
        is ShoppingSessionRepositoryFailure.Unexpected -> ShowShoppingListResult.Unexpected(cause)
    }
}

sealed interface ShowShoppingListResult {
    data class Shown(val items: ImmutableList<ShoppingListItem>) : ShowShoppingListResult
    data object NoActiveSession : ShowShoppingListResult
    data class Unexpected(val cause: Throwable) : ShowShoppingListResult
}
