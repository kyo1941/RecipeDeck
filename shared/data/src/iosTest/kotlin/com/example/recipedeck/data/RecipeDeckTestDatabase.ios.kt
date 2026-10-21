package com.example.recipedeck.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

actual fun recipeDeckTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase> =
    Room.inMemoryDatabaseBuilder<RecipeDeckDatabase>()

@OptIn(ExperimentalForeignApi::class)
actual fun recipeDeckFileTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase> {
    val path = NSTemporaryDirectory() + "recipe-deck-test.db"
    listOf("", "-wal", "-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(path + it, null) }
    return Room.databaseBuilder<RecipeDeckDatabase>(path)
}
