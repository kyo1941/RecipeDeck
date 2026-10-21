package com.example.recipedeck.data

import androidx.room3.RoomDatabase
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

expect fun recipeDeckTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase>

expect fun recipeDeckFileTestDatabaseBuilder(): RoomDatabase.Builder<RecipeDeckDatabase>

fun runTestWithRecipeDeckDatabase(block: suspend TestScope.(RecipeDeckDatabase) -> Unit) =
    runTestWith(recipeDeckTestDatabaseBuilder(), block)

fun runTestWithRecipeDeckFileDatabase(block: suspend TestScope.(RecipeDeckDatabase) -> Unit) =
    runTestWith(recipeDeckFileTestDatabaseBuilder(), block)

private fun runTestWith(
    builder: RoomDatabase.Builder<RecipeDeckDatabase>,
    block: suspend TestScope.(RecipeDeckDatabase) -> Unit,
) = runTest {
    val database = buildRecipeDeckDatabase(builder)
    try {
        block(database)
    } finally {
        database.close()
    }
}
