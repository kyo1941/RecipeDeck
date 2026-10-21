package com.example.recipedeck.data

import androidx.room3.Room
import androidx.room3.RoomDatabase

actual fun recipeDeckTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase> =
    Room.inMemoryDatabaseBuilder<RecipeDeckDatabase>()

actual fun recipeDeckFileTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase> =
    freshFileTestDatabaseBuilder("recipe-deck-test.db")
