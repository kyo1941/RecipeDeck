package com.example.recipedeck.data.item

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "item", indices = [Index(value = ["display_name"], unique = true)])
internal data class ItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
)
