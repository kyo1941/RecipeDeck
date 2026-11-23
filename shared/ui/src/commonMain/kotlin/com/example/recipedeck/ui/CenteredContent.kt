package com.example.recipedeck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.load_retry
import com.example.recipedeck.ui.resources.storage_shortage_hint
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CenteredContent(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
internal fun LoadFailedContent(message: String, onRetry: () -> Unit, modifier: Modifier) {
    CenteredContent(modifier) {
        Text(message)
        Text(stringResource(Res.string.storage_shortage_hint))
        Button(onClick = onRetry) { Text(stringResource(Res.string.load_retry)) }
    }
}
