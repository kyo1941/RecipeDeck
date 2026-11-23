package com.example.recipedeck.ui.recipe

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recipedeck.ui.CenteredContent
import com.example.recipedeck.ui.LoadFailedContent
import com.example.recipedeck.ui.amount.amountText
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.navigation_back
import com.example.recipedeck.ui.resources.recipe_draft
import com.example.recipedeck.ui.resources.recipe_ingredients
import com.example.recipedeck.ui.resources.recipe_load_failed
import com.example.recipedeck.ui.resources.recipe_memo
import com.example.recipedeck.ui.resources.recipe_no_ingredients
import com.example.recipedeck.ui.resources.recipe_no_longer_exists
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(viewModel: RecipeViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text((uiState as? RecipeUiState.Shown)?.name.orEmpty()) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(Res.string.navigation_back)) } },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (val state = uiState) {
            RecipeUiState.Loading -> CenteredContent(contentModifier) { CircularProgressIndicator() }
            is RecipeUiState.Shown -> RecipeContent(state, contentModifier)
            RecipeUiState.NoLongerExists -> CenteredContent(contentModifier) { Text(stringResource(Res.string.recipe_no_longer_exists)) }
            RecipeUiState.LoadFailed -> LoadFailedContent(stringResource(Res.string.recipe_load_failed), viewModel::retry, contentModifier)
        }
    }
}

@Composable
private fun RecipeContent(state: RecipeUiState.Shown, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize()) {
        if (state.isDraft) {
            item { ListItem(headlineContent = { Text(stringResource(Res.string.recipe_draft)) }) }
        }
        item { SectionTitle(stringResource(Res.string.recipe_ingredients)) }
        if (state.ingredients.isEmpty()) {
            item { ListItem(headlineContent = { Text(stringResource(Res.string.recipe_no_ingredients)) }) }
        }
        items(state.ingredients) { ingredient ->
            ListItem(
                headlineContent = { Text(ingredient.name) },
                trailingContent = ingredient.amount?.let { amount -> { Text(amountText(amount)) } },
            )
        }
        state.memo?.let { memo ->
            item { SectionTitle(stringResource(Res.string.recipe_memo)) }
            item { ListItem(headlineContent = { Text(memo) }) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    ListItem(headlineContent = { Text(text, style = MaterialTheme.typography.titleSmall) })
}
