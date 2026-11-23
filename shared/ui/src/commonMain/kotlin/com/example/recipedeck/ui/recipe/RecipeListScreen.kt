package com.example.recipedeck.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.ui.CenteredContent
import com.example.recipedeck.ui.LoadFailedContent
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.recipe_list_draft
import com.example.recipedeck.ui.resources.recipe_list_load_failed
import com.example.recipedeck.ui.resources.recipe_list_no_recipes
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeListScreen(viewModel: RecipeListViewModel, onRecipeClick: (RecipeId) -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        RecipeListUiState.Loading -> CenteredContent(modifier) { CircularProgressIndicator() }
        RecipeListUiState.NoRecipes -> CenteredContent(modifier) { Text(stringResource(Res.string.recipe_list_no_recipes)) }
        is RecipeListUiState.Shown -> LazyColumn(modifier.fillMaxSize()) {
            items(state.items, key = { it.id.value }) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    modifier = Modifier.clickable { onRecipeClick(item.id) },
                    trailingContent = if (item.isDraft) {
                        { Text(stringResource(Res.string.recipe_list_draft)) }
                    } else {
                        null
                    },
                )
            }
        }
        RecipeListUiState.LoadFailed -> LoadFailedContent(stringResource(Res.string.recipe_list_load_failed), viewModel::retry, modifier)
    }
}
