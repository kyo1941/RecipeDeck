package com.example.recipedeck.data.shopping

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "session_recipe",
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
internal data class SessionRecipeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "source_recipe_id") val sourceRecipeId: String,
    @ColumnInfo(name = "recipe_name") val recipeName: String,
    @ColumnInfo(name = "multiplier_numerator") val multiplierNumerator: Long,
    @ColumnInfo(name = "multiplier_denominator") val multiplierDenominator: Long,
)
