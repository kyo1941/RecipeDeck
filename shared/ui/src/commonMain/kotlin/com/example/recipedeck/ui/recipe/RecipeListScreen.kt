package com.example.recipedeck.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.ui.CenteredContent
import com.example.recipedeck.ui.LoadFailedContent
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.leftover_new_recipe
import com.example.recipedeck.ui.resources.leftover_resume
import com.example.recipedeck.ui.resources.recipe_list_create
import com.example.recipedeck.ui.resources.recipe_list_draft
import com.example.recipedeck.ui.resources.recipe_list_load_failed
import com.example.recipedeck.ui.resources.recipe_list_no_recipes
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onRecipeClick: (RecipeId) -> Unit,
    onCreateRecipeClick: () -> Unit,
    onResumeLeftoverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasLeftoverNewRecipe by viewModel.hasLeftoverNewRecipe.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreateRecipeClick) { Text(stringResource(Res.string.recipe_list_create)) }
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            if (hasLeftoverNewRecipe) {
                LeftoverEditBanner(
                    message = stringResource(Res.string.leftover_new_recipe),
                    useLabel = stringResource(Res.string.leftover_resume),
                    onUse = onResumeLeftoverClick,
                    onDiscard = viewModel::discardLeftoverNewRecipe,
                )
            }
            RecipeListContent(uiState, viewModel::retry, onRecipeClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecipeListContent(
    uiState: RecipeListUiState,
    onRetry: () -> Unit,
    onRecipeClick: (RecipeId) -> Unit,
    contentModifier: Modifier,
) {
    when (val state = uiState) {
        RecipeListUiState.Loading -> CenteredContent(contentModifier) { CircularProgressIndicator() }
        RecipeListUiState.NoRecipes -> CenteredContent(contentModifier) { Text(stringResource(Res.string.recipe_list_no_recipes)) }
        is RecipeListUiState.Shown -> LazyColumn(contentModifier.fillMaxSize()) {
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
        RecipeListUiState.LoadFailed -> LoadFailedContent(stringResource(Res.string.recipe_list_load_failed), onRetry, contentModifier)
    }
}
