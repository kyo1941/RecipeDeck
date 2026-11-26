package com.example.recipedeck.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.ui.navigation.RecipeDeckNavigation
import com.example.recipedeck.ui.navigation.Route
import com.example.recipedeck.ui.navigation.TopLevelDestination
import com.example.recipedeck.ui.navigation.rememberRecipeDeckNavigation
import com.example.recipedeck.ui.navigation.withoutAnimationOnDestinationChange
import com.example.recipedeck.ui.recipe.CreateRecipeScreenHolder
import com.example.recipedeck.ui.recipe.EditRecipeScreenHolder
import com.example.recipedeck.ui.recipe.RecipeListScreenHolder
import com.example.recipedeck.ui.recipe.RecipeScreenHolder
import com.example.recipedeck.ui.shopping.ShoppingListScreenHolder
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeDeckApp(viewModels: ScreenViewModels) {
    MaterialTheme {
        val navigation = rememberRecipeDeckNavigation()
        Scaffold(bottomBar = { TopLevelNavigationBar(navigation) }) { innerPadding ->
            NavDisplay(
                entries = navigation.displayedEntries { route -> screenOf(route, viewModels, navigation) },
                sceneStrategies = listOf(DialogSceneStrategy(), SinglePaneSceneStrategy()),
                modifier = Modifier.padding(innerPadding),
                transitionSpec = withoutAnimationOnDestinationChange(defaultTransitionSpec()),
                popTransitionSpec = withoutAnimationOnDestinationChange(defaultPopTransitionSpec()),
                onBack = navigation::goBack,
            )
        }
    }
}

private fun screenOf(route: Route, viewModels: ScreenViewModels, navigation: RecipeDeckNavigation): NavEntry<Route> = when (route) {
    Route.RecipeList -> NavEntry(route) {
        RecipeListScreenHolder(
            viewModel = viewModel { viewModels.recipeListViewModel },
            onRecipeClick = { recipeId -> navigation.open(Route.Recipe(recipeId.value)) },
            onCreateRecipeClick = { navigation.open(Route.CreateRecipe()) },
            onResumeLeftoverClick = { navigation.open(Route.CreateRecipe(resumesLeftover = true)) },
        )
    }
    Route.ShoppingList -> NavEntry(route) { ShoppingListScreenHolder(viewModel { viewModels.shoppingListViewModel }) }
    is Route.Recipe -> NavEntry(route) {
        RecipeScreenHolder(
            viewModel = viewModel { viewModels.recipeViewModelFactory.create(RecipeId(route.recipeId)) },
            onBack = navigation::goBack,
            onEditClick = { navigation.open(Route.EditRecipe(route.recipeId)) },
        )
    }
    is Route.CreateRecipe -> NavEntry(route, metadata = DialogSceneStrategy.dialog(FullScreenModalProperties)) {
        CreateRecipeScreenHolder(
            viewModel = viewModel { viewModels.createRecipeViewModelFactory.create(route.resumesLeftover) },
            onClosed = navigation::goBack,
        )
    }
    is Route.EditRecipe -> NavEntry(route, metadata = DialogSceneStrategy.dialog(FullScreenModalProperties)) {
        EditRecipeScreenHolder(
            viewModel = viewModel { viewModels.editRecipeViewModelFactory.create(RecipeId(route.recipeId)) },
            onClosed = navigation::goBack,
        )
    }
}

private val FullScreenModalProperties = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
)

@Composable
private fun TopLevelNavigationBar(navigation: RecipeDeckNavigation) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = navigation.currentDestination == destination,
                onClick = { navigation.select(destination) },
                icon = {},
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}
