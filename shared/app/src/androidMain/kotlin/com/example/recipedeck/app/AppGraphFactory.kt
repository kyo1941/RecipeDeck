package com.example.recipedeck.app

import android.content.Context
import com.example.recipedeck.data.recipeDeckDatabaseBuilder
import dev.zacsweers.metro.createGraphFactory

fun createAppGraph(context: Context): AppGraph =
    createGraphFactory<AppGraph.Factory>().create(recipeDeckDatabaseBuilder(context))
