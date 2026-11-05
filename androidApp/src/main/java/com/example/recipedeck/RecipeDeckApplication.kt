package com.example.recipedeck

import android.app.Application
import com.example.recipedeck.app.AppGraph
import com.example.recipedeck.app.createAppGraph

class RecipeDeckApplication : Application() {
    val appGraph: AppGraph by lazy { createAppGraph(this) }
}
