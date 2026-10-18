package com.example.recipedeck.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.test.core.app.ApplicationProvider

actual fun transactionTestDatabaseBuilder(): RoomDatabase.Builder<TransactionTestDatabase> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val name = "transaction-test.db"
    context.deleteDatabase(name)
    return Room.databaseBuilder<TransactionTestDatabase>(context, name)
}
