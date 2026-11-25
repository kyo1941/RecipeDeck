package com.example.recipedeck.app

import androidx.compose.ui.window.ComposeUIViewController
import com.example.recipedeck.data.recipeDeckDatabaseBuilder
import dev.zacsweers.metro.createGraphFactory
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private val appGraph: AppGraph by lazy {
    createGraphFactory<AppGraph.Factory>().create(
        databaseBuilder = recipeDeckDatabaseBuilder(),
        leftoverRecipeEditStore = DataStoreLeftoverRecipeEditStore("${applicationSupportPath()}/$LEFTOVER_RECIPE_EDITS_FILE_NAME"),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportPath(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(directory?.path) { "Application Support のフォルダを開けない" }
}

fun MainViewController() = ComposeUIViewController { App(appGraph) }
