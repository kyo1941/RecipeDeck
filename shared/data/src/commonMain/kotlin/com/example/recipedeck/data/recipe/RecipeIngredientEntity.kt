package com.example.recipedeck.data.recipe

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.example.recipedeck.data.amount.AmountColumns
import com.example.recipedeck.data.item.ItemEntity

@Entity(
    tableName = "recipe_ingredient",
    primaryKeys = ["recipe_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["item_id"])],
)
internal data class RecipeIngredientEntity(
    @ColumnInfo(name = "recipe_id") val recipeId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "item_id") val itemId: String,
    @Embedded val amount: AmountColumns,
)
