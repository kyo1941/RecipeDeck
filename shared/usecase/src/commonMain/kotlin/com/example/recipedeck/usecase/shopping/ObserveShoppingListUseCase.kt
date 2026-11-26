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

fun interface ObserveShoppingListUseCase {
    operator fun invoke(): Flow<ObserveShoppingListResult>
}

fun ObserveShoppingListUseCase(
    shoppingSessionRepository: ShoppingSessionRepository,
    purchaseRecordRepository: PurchaseRecordRepository,
): ObserveShoppingListUseCase = object : ObserveShoppingListUseCase {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<ObserveShoppingListResult> =
        shoppingSessionRepository.observeActive().flatMapLatest { found ->
            val session = when (found) {
                is Outcome.Success -> found.value ?: return@flatMapLatest flowOf(ObserveShoppingListResult.NoActiveSession)
                is Outcome.Failure -> return@flatMapLatest flowOf(found.error.toObserveShoppingListResult())
            }
            purchaseRecordRepository.observePurchasedSourceIds(session.id).map { purchased ->
                when (purchased) {
                    is Outcome.Success -> ObserveShoppingListResult.Listed(session.toShoppingList(purchased.value))
                    is Outcome.Failure -> purchased.error.toObserveShoppingListResult()
                }
            }
        }

    private fun ShoppingSessionRepositoryFailure.toObserveShoppingListResult(): ObserveShoppingListResult = when (this) {
        is ShoppingSessionRepositoryFailure.Unexpected -> ObserveShoppingListResult.Unexpected(cause)
    }

    private fun PurchaseRecordRepositoryFailure.toObserveShoppingListResult(): ObserveShoppingListResult = when (this) {
        is PurchaseRecordRepositoryFailure.Unexpected -> ObserveShoppingListResult.Unexpected(cause)
    }
}

sealed interface ObserveShoppingListResult {
    data class Listed(val items: ImmutableList<ShoppingListItem>) : ObserveShoppingListResult
    data object NoActiveSession : ObserveShoppingListResult
    data class Unexpected(val cause: Throwable) : ObserveShoppingListResult
}
