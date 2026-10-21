package com.example.recipedeck.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun recipeDeckDatabaseBuilder(context: Context): RoomDatabase.Builder<RecipeDeckDatabase> =
    Room.databaseBuilder<RecipeDeckDatabase>(context.applicationContext, RECIPE_DECK_DATABASE_FILE_NAME)
