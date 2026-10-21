package com.example.recipedeck.data.item

import androidx.room3.Dao
import androidx.room3.Query

@Dao
internal interface ItemDao {
    /**
     * Room の `@Upsert` は、表示名の一意制約にぶつかっても主キーでの更新に切り替え、0行の更新で黙って成功する。
     * そのため、ID がぶつかったときだけ上書きし、表示名がぶつかったら失敗する形を SQL で書いている。
     */
    @Query(
        """
        INSERT INTO item (id, display_name) VALUES (:id, :displayName)
        ON CONFLICT(id) DO UPDATE SET display_name = excluded.display_name
        """,
    )
    suspend fun save(id: String, displayName: String)

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun findById(id: String): ItemEntity?

    @Query("SELECT * FROM item WHERE id IN (:ids) ORDER BY rowid")
    suspend fun findByIds(ids: List<String>): List<ItemEntity>

    @Query("SELECT * FROM item WHERE display_name = :displayName")
    suspend fun findByDisplayName(displayName: String): ItemEntity?

    @Query("SELECT * FROM item WHERE display_name IN (:displayNames) ORDER BY rowid")
    suspend fun findByDisplayNames(displayNames: List<String>): List<ItemEntity>

    @Query(
        """
        SELECT * FROM item WHERE instr(display_name, :keyword) > 0
        ORDER BY
            CASE
                WHEN display_name = :keyword THEN 0
                WHEN instr(display_name, :keyword) = 1 THEN 1
                ELSE 2
            END,
            length(display_name),
            rowid
        """,
    )
    suspend fun search(keyword: String): List<ItemEntity>
}
