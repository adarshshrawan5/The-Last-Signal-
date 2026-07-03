package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapter_progress")
data class ChapterProgress(
    @PrimaryKey val chapterNumber: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val choiceMade: String = "", // e.g. "Destroy" or "Answer" in Chapter 7
    val completedAt: Long = 0L
)

@Entity(tableName = "clues")
data class Clue(
    @PrimaryKey val id: String,
    val chapterNumber: Int,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val userNote: String = "",
    val isCustom: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val foundInChapter: Int,
    val isFound: Boolean = false,
    val foundAt: Long = 0L
)
