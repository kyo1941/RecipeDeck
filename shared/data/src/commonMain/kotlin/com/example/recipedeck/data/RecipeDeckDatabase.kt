package com.example.recipedeck.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.recipedeck.data.item.ItemDao
import com.example.recipedeck.data.item.ItemEntity
import com.example.recipedeck.data.recipe.RecipeDao
import com.example.recipedeck.data.recipe.RecipeEntity
import com.example.recipedeck.data.recipe.RecipeIngredientEntity

@Database(entities = [ItemEntity::class, RecipeEntity::class, RecipeIngredientEntity::class], version = 1, exportSchema = false)
@ConstructedBy(RecipeDeckDatabaseConstructor::class)
abstract class RecipeDeckDatabase : RoomDatabase() {
    internal abstract fun itemDao(): ItemDao
    internal abstract fun recipeDao(): RecipeDao
}

@Suppress("KotlinNoActualForExpect")
expect object RecipeDeckDatabaseConstructor : RoomDatabaseConstructor<RecipeDeckDatabase> {
    override fun initialize(): RecipeDeckDatabase
}

internal const val RECIPE_DECK_DATABASE_FILE_NAME = "recipe-deck.db"

fun buildRecipeDeckDatabase(builder: RoomDatabase.Builder<RecipeDeckDatabase>): RecipeDeckDatabase =
    builder.setDriver(BundledSQLiteDriver()).build()
