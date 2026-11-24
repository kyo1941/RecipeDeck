package com.example.recipedeck.ui.navigation

import androidx.compose.runtime.Composable

@Composable
internal expect fun ModalCloseButton(onCloseRequest: () -> Unit, enabled: Boolean)

/**
 * 戻る操作でモーダルを閉じるかは OS の慣習で違う。
 * 何も受けないと、モーダルの下の NavDisplay が戻る操作を受けてモーダルを外してしまう。
 */
@Composable
internal expect fun ModalBackHandler(onCloseRequest: () -> Unit)
