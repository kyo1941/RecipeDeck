package com.example.recipedeck.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

@Composable
internal fun ScreenPreview(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(content = content)
    }
}
