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
import com.example.recipedeck.ui.navigation.RecipeDeckNavigation
import com.example.recipedeck.ui.navigation.ReturnToStartDestinationOnBack
import com.example.recipedeck.ui.navigation.Route
import com.example.recipedeck.ui.navigation.TopLevelDestination
import com.example.recipedeck.ui.navigation.forwardTransitionExceptDestinationChange
import com.example.recipedeck.ui.navigation.rememberRecipeDeckNavigation
import com.example.recipedeck.ui.recipe.RecipeListScreen
import com.example.recipedeck.ui.shopping.ShoppingListScreen
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeDeckApp(viewModels: ScreenViewModels) {
    MaterialTheme {
        val navigation = rememberRecipeDeckNavigation()
        ReturnToStartDestinationOnBack(
            enabled = navigation.isAtRootOfNonStartDestination,
            onBack = { navigation.select(TopLevelDestination.START) },
        )
        Scaffold(bottomBar = { TopLevelNavigationBar(navigation) }) { innerPadding ->
            NavDisplay(
                entries = navigation.currentEntries { route -> screenOf(route, viewModels) },
                modifier = Modifier.padding(innerPadding),
                transitionSpec = forwardTransitionExceptDestinationChange(),
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
