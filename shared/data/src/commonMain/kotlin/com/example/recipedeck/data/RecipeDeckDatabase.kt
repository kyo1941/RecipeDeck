package com.example.recipedeck.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.recipedeck.data.item.ItemDao
import com.example.recipedeck.data.item.ItemEntity
import com.example.recipedeck.data.recipe.RecipeDao
import com.example.recipedeck.data.recipe.RecipeEntity
import com.example.recipedeck.data.recipe.RecipeIngredientEntity
import com.example.recipedeck.data.shopping.PurchaseRecordDao
import com.example.recipedeck.data.shopping.PurchaseRecordEntity
import com.example.recipedeck.data.shopping.SessionItemEntity
import com.example.recipedeck.data.shopping.SessionRecipeEntity
import com.example.recipedeck.data.shopping.SessionRecipeIngredientEntity
import com.example.recipedeck.data.shopping.ShoppingSessionDao
import com.example.recipedeck.data.shopping.ShoppingSessionEntity

@Database(
    entities = [
        ItemEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        ShoppingSessionEntity::class,
        SessionRecipeEntity::class,
        SessionItemEntity::class,
        SessionRecipeIngredientEntity::class,
        PurchaseRecordEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(RecipeDeckDatabaseConstructor::class)
abstract class RecipeDeckDatabase : RoomDatabase() {
    internal abstract fun itemDao(): ItemDao
    internal abstract fun recipeDao(): RecipeDao
    internal abstract fun shoppingSessionDao(): ShoppingSessionDao
    internal abstract fun purchaseRecordDao(): PurchaseRecordDao
}

@Suppress("KotlinNoActualForExpect")
expect object RecipeDeckDatabaseConstructor : RoomDatabaseConstructor<RecipeDeckDatabase> {
    override fun initialize(): RecipeDeckDatabase
}

internal const val RECIPE_DECK_DATABASE_FILE_NAME = "recipe-deck.db"

fun buildRecipeDeckDatabase(builder: RoomDatabase.Builder<RecipeDeckDatabase>): RecipeDeckDatabase =
    builder.setDriver(BundledSQLiteDriver()).addCallback(OneActiveShoppingSessionTriggers).build()

/**
 * 進行中の Session を最大1件に保つ。
 * Room の `@Index` は進行中の行だけに張る一意索引を書けず、Room の外で作った索引は移行のときの検証で食い違うため、Room が検証しないトリガーで守る。
 * Room はこのトリガーを作らないので、データベースを作るときに作る。表を作り直す移行では、トリガーも作り直す。
 */
private object OneActiveShoppingSessionTriggers : RoomDatabase.Callback() {
    override suspend fun onCreate(connection: SQLiteConnection) {
        connection.execSQL(createTrigger(name = "one_active_shopping_session_before_insert", event = "INSERT"))
        connection.execSQL(createTrigger(name = "one_active_shopping_session_before_update", event = "UPDATE OF completed_at_epoch_millis"))
    }

    private fun createTrigger(name: String, event: String): String =
        """
        CREATE TRIGGER $name BEFORE $event ON shopping_session
        WHEN NEW.completed_at_epoch_millis IS NULL
            AND EXISTS (SELECT 1 FROM shopping_session WHERE completed_at_epoch_millis IS NULL AND id <> NEW.id)
        BEGIN SELECT RAISE(ABORT, '進行中の Session はすでにある'); END
        """
}
