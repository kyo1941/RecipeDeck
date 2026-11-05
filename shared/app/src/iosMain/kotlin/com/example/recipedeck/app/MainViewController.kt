package com.example.recipedeck.app

import androidx.compose.ui.window.ComposeUIViewController
import com.example.recipedeck.data.recipeDeckDatabaseBuilder
import dev.zacsweers.metro.createGraphFactory

private val appGraph: AppGraph by lazy {
    createGraphFactory<AppGraph.Factory>().create(recipeDeckDatabaseBuilder())
}

fun MainViewController() = ComposeUIViewController { App(appGraph) }
