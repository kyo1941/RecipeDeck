package com.example.recipedeck.ui.shopping

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recipedeck.ui.CenteredContent
import com.example.recipedeck.ui.LoadFailedContent
import com.example.recipedeck.ui.ScreenPreview
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.shopping_list_load_failed
import com.example.recipedeck.ui.resources.shopping_list_no_active_session
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShoppingListScreenHolder(viewModel: ShoppingListViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ShoppingListScreen(uiState, onRetry = viewModel::retry, modifier = modifier)
}

@Composable
fun ShoppingListScreen(uiState: ShoppingListUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    when (val state = uiState) {
        ShoppingListUiState.Loading -> CenteredContent(modifier) { CircularProgressIndicator() }
        ShoppingListUiState.NoActiveSession -> CenteredContent(modifier) { Text(stringResource(Res.string.shopping_list_no_active_session)) }
        is ShoppingListUiState.Shown -> LazyColumn(modifier.fillMaxSize()) {
            items(state.itemNames) { itemName -> Text(itemName) }
        }
        ShoppingListUiState.LoadFailed -> LoadFailedContent(stringResource(Res.string.shopping_list_load_failed), onRetry, modifier)
    }
}

@Preview
@Composable
private fun ShoppingListScreenListedPreview() {
    ScreenPreview {
        ShoppingListScreen(ShoppingListUiState.Shown(persistentListOf("長ネギ", "豆腐", "豚こま肉")), onRetry = {})
    }
}

@Preview
@Composable
private fun ShoppingListScreenNoActiveSessionPreview() {
    ScreenPreview {
        ShoppingListScreen(ShoppingListUiState.NoActiveSession, onRetry = {})
    }
}

@Preview
@Composable
private fun ShoppingListScreenLoadFailedPreview() {
    ScreenPreview {
        ShoppingListScreen(ShoppingListUiState.LoadFailed, onRetry = {})
    }
}
