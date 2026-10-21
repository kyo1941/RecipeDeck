package com.example.recipedeck.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.test.core.app.ApplicationProvider

internal inline fun <reified T : RoomDatabase> freshFileTestDatabaseBuilder(name: String): RoomDatabase.Builder<T> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.deleteDatabase(name)
    return Room.databaseBuilder<T>(context, name)
}
