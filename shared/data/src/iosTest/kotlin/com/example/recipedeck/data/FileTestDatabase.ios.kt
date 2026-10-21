package com.example.recipedeck.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

@OptIn(ExperimentalForeignApi::class)
internal inline fun <reified T : RoomDatabase> freshFileTestDatabaseBuilder(name: String): RoomDatabase.Builder<T> {
    val path = NSTemporaryDirectory() + name
    listOf("", "-wal", "-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(path + it, null) }
    return Room.databaseBuilder<T>(path)
}
