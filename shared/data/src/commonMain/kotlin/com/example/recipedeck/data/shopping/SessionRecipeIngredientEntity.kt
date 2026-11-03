package com.example.recipedeck.data.shopping

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.example.recipedeck.data.amount.AmountColumns

@Entity(
    tableName = "session_recipe_ingredient",
    foreignKeys = [
        ForeignKey(
            entity = SessionRecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SessionItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_item_id"],
        ),
    ],
    indices = [Index(value = ["session_recipe_id"]), Index(value = ["session_item_id"])],
)
internal data class SessionRecipeIngredientEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_recipe_id") val sessionRecipeId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "session_item_id") val sessionItemId: String,
    @Embedded val amount: AmountColumns,
    @ColumnInfo(name = "is_excluded") val isExcluded: Boolean,
)
