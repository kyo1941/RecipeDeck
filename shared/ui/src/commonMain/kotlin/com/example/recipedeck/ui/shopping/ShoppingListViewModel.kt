package com.example.recipedeck.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipedeck.usecase.shopping.ShowShoppingListResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    showShoppingList: suspend () -> ShowShoppingListResult,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ShoppingListUiState>(ShoppingListUiState.Loading)
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = showShoppingList().toUiState()
        }
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
