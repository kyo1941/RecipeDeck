package com.example.recipedeck.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.recipedeck.usecase.shopping.ShowShoppingListResult
import com.example.recipedeck.usecase.shopping.ShowShoppingListUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class ShoppingListViewModel(
    showShoppingList: ShowShoppingListUseCase,
) : ViewModel() {
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ShoppingListUiState> = retryRequests
        .onStart { emit(Unit) }
        .flatMapLatest { showShoppingList() }
        .onEach { if (it is ShowShoppingListResult.Unexpected) logger.e(it.cause) { "買い物リストを読み込めなかった" } }
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShoppingListUiState.Loading)

    fun retry() {
        retryRequests.tryEmit(Unit)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val logger = Logger.withTag("ShoppingListViewModel")
    }
}

sealed interface ShoppingListUiState {
    data object Loading : ShoppingListUiState
    data object NoActiveSession : ShoppingListUiState
    data class Shown(val itemNames: ImmutableList<String>) : ShoppingListUiState
    data object LoadFailed : ShoppingListUiState
}

private fun ShowShoppingListResult.toUiState(): ShoppingListUiState = when (this) {
    is ShowShoppingListResult.Shown -> ShoppingListUiState.Shown(items.map { it.displayName.value }.toImmutableList())
    ShowShoppingListResult.NoActiveSession -> ShoppingListUiState.NoActiveSession
    is ShowShoppingListResult.Unexpected -> ShoppingListUiState.LoadFailed
}
