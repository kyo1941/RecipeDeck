package com.example.recipedeck.ui.navigation

import androidx.compose.runtime.Composable

@Composable
internal expect fun ModalCloseButton(onCloseRequest: () -> Unit, enabled: Boolean)

@Composable
internal expect fun ModalBackHandler(onCloseRequest: () -> Unit)
