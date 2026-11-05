package com.example.recipedeck.ui.shopping

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        ShoppingListUiState.Loading -> CenteredContent(modifier) { CircularProgressIndicator() }
        ShoppingListUiState.NoActiveSession -> CenteredContent(modifier) { Text("買い物リストはまだありません") }
        is ShoppingListUiState.Shown -> LazyColumn(modifier.fillMaxSize()) {
            items(state.itemNames) { itemName -> Text(itemName) }
        }
        ShoppingListUiState.LoadFailed -> CenteredContent(modifier) { Text("買い物リストを読み込めませんでした") }
    }
}

@Composable
private fun CenteredContent(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}
