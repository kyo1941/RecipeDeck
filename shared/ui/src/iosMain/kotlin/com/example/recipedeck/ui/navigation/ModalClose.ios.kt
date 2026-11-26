package com.example.recipedeck.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.modal_cancel
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun ModalCloseButton(onCloseRequest: () -> Unit, enabled: Boolean) {
    TextButton(onClick = onCloseRequest, enabled = enabled) { Text(stringResource(Res.string.modal_cancel)) }
}

@Composable
internal actual fun ModalBackHandler(onCloseRequest: () -> Unit) {
    // iOS では戻る操作でモーダルを閉じないが、ここで消費しないと下の NavDisplay に抜けて閉じてしまう。
    NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = {})
}
