package com.example.recipedeck.data.shopping

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = ShoppingSessionEntity.TABLE_NAME)
internal data class ShoppingSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "completed_at_epoch_millis") val completedAtEpochMillis: Long?,
) {
    companion object {
        const val TABLE_NAME = "shopping_session"
    }
}
