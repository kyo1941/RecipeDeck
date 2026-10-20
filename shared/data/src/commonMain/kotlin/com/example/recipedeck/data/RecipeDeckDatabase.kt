package com.example.recipedeck.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.example.recipedeck.data.item.ItemDao
import com.example.recipedeck.data.item.ItemEntity

@Database(entities = [ItemEntity::class], version = 1, exportSchema = false)
@ConstructedBy(RecipeDeckDatabaseConstructor::class)
abstract class RecipeDeckDatabase : RoomDatabase() {
    internal abstract fun itemDao(): ItemDao
}

@Suppress("KotlinNoActualForExpect")
expect object RecipeDeckDatabaseConstructor : RoomDatabaseConstructor<RecipeDeckDatabase> {
    override fun initialize(): RecipeDeckDatabase
}
