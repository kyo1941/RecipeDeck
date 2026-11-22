package com.example.recipedeck.ui.navigation

import androidx.compose.runtime.Composable

@Composable
internal expect fun ReturnToStartDestinationOnBack(enabled: Boolean, onBack: () -> Unit)
