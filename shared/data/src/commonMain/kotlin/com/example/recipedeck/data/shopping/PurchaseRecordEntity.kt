package com.example.recipedeck.data.shopping

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = PurchaseRecordEntity.TABLE_NAME,
    primaryKeys = ["session_id", "source_id"],
    foreignKeys = [
        ForeignKey(
            entity = ShoppingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
        ),
    ],
)
internal data class PurchaseRecordEntity(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
) {
    companion object {
        const val TABLE_NAME = "purchase_record"
    }
}
