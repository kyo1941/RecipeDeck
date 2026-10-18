package com.example.recipedeck.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

@OptIn(ExperimentalForeignApi::class)
actual fun transactionTestDatabaseBuilder(): RoomDatabase.Builder<TransactionTestDatabase> {
    val path = NSTemporaryDirectory() + "transaction-test.db"
    listOf("", "-wal", "-shm").forEach { NSFileManager.defaultManager.removeItemAtPath(path + it, null) }
    return Room.databaseBuilder<TransactionTestDatabase>(path)
}
