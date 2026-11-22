package com.example.recipedeck.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import com.example.recipedeck.ui.navigation.RecipeDeckNavigation
import com.example.recipedeck.ui.navigation.Route
import com.example.recipedeck.ui.navigation.TopLevelDestination
import com.example.recipedeck.ui.navigation.rememberRecipeDeckNavigation
import com.example.recipedeck.ui.navigation.withoutAnimationOnDestinationChange
import com.example.recipedeck.ui.recipe.RecipeListScreen
import com.example.recipedeck.ui.shopping.ShoppingListScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeDeckApp(viewModels: ScreenViewModels) {
    MaterialTheme {
        val navigation = rememberRecipeDeckNavigation()
        Scaffold(bottomBar = { TopLevelNavigationBar(navigation) }) { innerPadding ->
            NavDisplay(
                entries = navigation.displayedEntries { route -> screenOf(route, viewModels) },
                modifier = Modifier.padding(innerPadding),
                transitionSpec = withoutAnimationOnDestinationChange(defaultTransitionSpec()),
                popTransitionSpec = withoutAnimationOnDestinationChange(defaultPopTransitionSpec()),
                onBack = navigation::goBack,
            )
        }
    }
}

private fun screenOf(route: Route, viewModels: ScreenViewModels): NavEntry<Route> = when (route) {
    Route.RecipeList -> NavEntry(route) { RecipeListScreen(viewModel { viewModels.recipeListViewModel }) }
    Route.ShoppingList -> NavEntry(route) { ShoppingListScreen(viewModel { viewModels.shoppingListViewModel }) }
}

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
