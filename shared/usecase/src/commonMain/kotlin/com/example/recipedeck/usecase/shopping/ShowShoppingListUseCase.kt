package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.shopping.PurchaseRecordRepository
import com.example.recipedeck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipedeck.domain.shopping.ShoppingListItem
import com.example.recipedeck.domain.shopping.ShoppingSessionRepository
import com.example.recipedeck.domain.shopping.ShoppingSessionRepositoryFailure
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

fun interface ShowShoppingListUseCase {
    operator fun invoke(): Flow<ShowShoppingListResult>
}

fun ShowShoppingListUseCase(
    shoppingSessionRepository: ShoppingSessionRepository,
    purchaseRecordRepository: PurchaseRecordRepository,
): ShowShoppingListUseCase = object : ShowShoppingListUseCase {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<ShowShoppingListResult> =
        shoppingSessionRepository.observeActive().flatMapLatest { found ->
            val session = when (found) {
                is Outcome.Success -> found.value ?: return@flatMapLatest flowOf(ShowShoppingListResult.NoActiveSession)
                is Outcome.Failure -> return@flatMapLatest flowOf(found.error.toShowShoppingListResult())
            }
            purchaseRecordRepository.observePurchasedSourceIds(session.id).map { purchased ->
                when (purchased) {
                    is Outcome.Success -> ShowShoppingListResult.Shown(session.toShoppingList(purchased.value))
                    is Outcome.Failure -> purchased.error.toShowShoppingListResult()
                }
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
