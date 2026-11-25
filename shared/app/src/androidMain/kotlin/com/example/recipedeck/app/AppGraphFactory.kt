package com.example.recipedeck.app

import android.content.Context
import com.example.recipedeck.data.recipeDeckDatabaseBuilder
import dev.zacsweers.metro.createGraphFactory

fun createAppGraph(context: Context): AppGraph = createGraphFactory<AppGraph.Factory>().create(
    databaseBuilder = recipeDeckDatabaseBuilder(context),
    leftoverRecipeEditStore = DataStoreLeftoverRecipeEditStore(context.filesDir.resolve(LEFTOVER_RECIPE_EDITS_FILE_NAME).absolutePath),
)
