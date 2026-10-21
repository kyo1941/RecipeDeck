package com.example.recipedeck.data

import androidx.room3.RoomDatabase

actual fun transactionTestDatabaseBuilder(): RoomDatabase.Builder<TransactionTestDatabase> =
    freshFileTestDatabaseBuilder("transaction-test.db")
