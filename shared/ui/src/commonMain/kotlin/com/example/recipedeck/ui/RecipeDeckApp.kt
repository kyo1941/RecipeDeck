package com.example.recipedeck.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipedeck.ui.shopping.ShoppingListScreen

@Composable
fun RecipeDeckApp(viewModels: ScreenViewModels) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ShoppingListScreen(
                viewModel = viewModel { viewModels.shoppingListViewModel },
                modifier = Modifier.safeDrawingPadding(),
            )
        }
    }
}
