package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanionDao {
    // Chapter Progress
    @Query("SELECT * FROM chapter_progress ORDER BY chapterNumber ASC")
    fun getAllChapterProgress(): Flow<List<ChapterProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapterProgress(progress: ChapterProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChapters(chapters: List<ChapterProgress>)

    @Query("UPDATE chapter_progress SET isCompleted = :isCompleted, choiceMade = :choiceMade, completedAt = :completedAt WHERE chapterNumber = :chapterNumber")
    suspend fun updateChapterStatus(chapterNumber: Int, isCompleted: Boolean, choiceMade: String, completedAt: Long)

    @Query("UPDATE chapter_progress SET isCompleted = 0, choiceMade = '', completedAt = 0")
    suspend fun resetAllChapters()

    // Clues & Notes
    @Query("SELECT * FROM clues ORDER BY timestamp DESC")
    fun getAllClues(): Flow<List<Clue>>

    @Query("SELECT * FROM clues WHERE chapterNumber = :chapterNumber ORDER BY timestamp DESC")
    fun getCluesByChapter(chapterNumber: Int): Flow<List<Clue>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClue(clue: Clue)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllClues(clues: List<Clue>)

    @Query("DELETE FROM clues WHERE id = :clueId")
    suspend fun deleteClue(clueId: String)

    @Query("UPDATE clues SET userNote = :note, isUnlocked = 1, timestamp = :timestamp WHERE id = :clueId")
    suspend fun updateClueNote(clueId: String, note: String, timestamp: Long)

    // Inventory Items
    @Query("SELECT * FROM inventory_items ORDER BY foundInChapter ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInventoryItems(items: List<InventoryItem>)

    @Query("UPDATE inventory_items SET isFound = :isFound, foundAt = :foundAt WHERE id = :itemId")
    suspend fun updateInventoryItemStatus(itemId: String, isFound: Boolean, foundAt: Long)

    @Query("UPDATE inventory_items SET isFound = 0, foundAt = 0")
    suspend fun resetAllInventory()
}
