package com.example.recipedeck.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.shopping_list_load_failed
import com.example.recipedeck.ui.resources.shopping_list_no_active_session
import com.example.recipedeck.ui.resources.shopping_list_retry
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        ShoppingListUiState.Loading -> CenteredContent(modifier) { CircularProgressIndicator() }
        ShoppingListUiState.NoActiveSession -> CenteredContent(modifier) { Text(stringResource(Res.string.shopping_list_no_active_session)) }
        is ShoppingListUiState.Shown -> LazyColumn(modifier.fillMaxSize()) {
            items(state.itemNames) { itemName -> Text(itemName) }
        }
        ShoppingListUiState.LoadFailed -> CenteredContent(modifier) {
            Text(stringResource(Res.string.shopping_list_load_failed))
            Button(onClick = viewModel::retry) { Text(stringResource(Res.string.shopping_list_retry)) }
        }
    }
}

@Composable
private fun CenteredContent(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
