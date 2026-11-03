package com.example.recipedeck.data.shopping

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.example.recipedeck.data.amount.AmountColumns

@Entity(
    tableName = "session_item",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id"])],
)
internal data class SessionItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "item_id") val itemId: String?,
    @ColumnInfo(name = "manual_source_id") val manualSourceId: String?,
    @Embedded(prefix = "manual_") val manualAmount: AmountColumns,
)
