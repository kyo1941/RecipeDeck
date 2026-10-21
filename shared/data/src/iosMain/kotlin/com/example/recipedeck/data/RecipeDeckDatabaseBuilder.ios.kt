package com.example.recipedeck.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun recipeDeckDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase> {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val directoryPath = requireNotNull(directory?.path) { "Application Support のフォルダを開けない" }
    return Room.databaseBuilder<RecipeDeckDatabase>("$directoryPath/$RECIPE_DECK_DATABASE_FILE_NAME")
}
