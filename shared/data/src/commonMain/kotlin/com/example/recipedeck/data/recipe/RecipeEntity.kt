package com.example.recipedeck.data.recipe

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "recipe")
internal data class RecipeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "photo_ref") val photoRef: String?,
    @ColumnInfo(name = "memo") val memo: String?,
)
