package com.example.recipedeck.data

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

expect fun recipeDeckTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase>

fun runTestWithRecipeDeckDatabase(block: suspend TestScope.(RecipeDeckDatabase) -> Unit) = runTest {
    val database = recipeDeckTestDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .build()
    try {
        block(database)
    } finally {
        database.close()
    }
}
