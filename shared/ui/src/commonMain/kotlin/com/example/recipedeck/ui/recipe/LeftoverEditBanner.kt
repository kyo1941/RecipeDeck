package com.example.recipedeck.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.leftover_discard
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LeftoverEditBanner(
    message: String,
    useLabel: String,
    onUse: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(message)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onUse, enabled = enabled) { Text(useLabel) }
                TextButton(onClick = onDiscard, enabled = enabled) { Text(stringResource(Res.string.leftover_discard)) }
            }
        }
    }
}
