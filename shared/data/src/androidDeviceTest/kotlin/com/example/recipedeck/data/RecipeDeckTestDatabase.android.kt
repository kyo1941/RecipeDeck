package com.example.recipedeck.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.test.core.app.ApplicationProvider

actual fun recipeDeckTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase> =
    Room.inMemoryDatabaseBuilder<RecipeDeckDatabase>()

actual fun recipeDeckFileTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val name = "recipe-deck-test.db"
    context.deleteDatabase(name)
    return Room.databaseBuilder<RecipeDeckDatabase>(context, name)
}
