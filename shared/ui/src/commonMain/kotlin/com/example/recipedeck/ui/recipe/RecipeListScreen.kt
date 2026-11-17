package com.example.recipedeck.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.load_retry
import com.example.recipedeck.ui.resources.recipe_list_draft
import com.example.recipedeck.ui.resources.recipe_list_load_failed
import com.example.recipedeck.ui.resources.recipe_list_no_recipes
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeListScreen(viewModel: RecipeListViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        RecipeListUiState.Loading -> CenteredContent(modifier) { CircularProgressIndicator() }
        RecipeListUiState.NoRecipes -> CenteredContent(modifier) { Text(stringResource(Res.string.recipe_list_no_recipes)) }
        is RecipeListUiState.Shown -> LazyColumn(modifier.fillMaxSize()) {
            items(state.items, key = { it.id.value }) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    trailingContent = if (item.isDraft) {
                        { Text(stringResource(Res.string.recipe_list_draft)) }
                    } else {
                        null
                    },
                )
            }
        }
        RecipeListUiState.LoadFailed -> CenteredContent(modifier) {
            Text(stringResource(Res.string.recipe_list_load_failed))
            Button(onClick = viewModel::retry) { Text(stringResource(Res.string.load_retry)) }
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
