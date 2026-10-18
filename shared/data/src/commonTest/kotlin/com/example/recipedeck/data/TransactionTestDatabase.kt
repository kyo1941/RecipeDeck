package com.example.recipedeck.data

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Entity
data class TestEntry(@PrimaryKey val id: String)

@Dao
interface TestEntryDao {
    @Insert
    suspend fun insert(entry: TestEntry)

    @Query("SELECT * FROM TestEntry")
    suspend fun findAll(): List<TestEntry>
}

@Database(entities = [TestEntry::class], version = 1, exportSchema = false)
@ConstructedBy(TransactionTestDatabaseConstructor::class)
abstract class TransactionTestDatabase : RoomDatabase() {
    abstract fun testEntryDao(): TestEntryDao
}

@Suppress("KotlinNoActualForExpect")
expect object TransactionTestDatabaseConstructor : RoomDatabaseConstructor<TransactionTestDatabase> {
    override fun initialize(): TransactionTestDatabase
}

expect fun transactionTestDatabaseBuilder(): RoomDatabase.Builder<TransactionTestDatabase>
