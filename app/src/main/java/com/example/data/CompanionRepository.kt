package com.example.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class CompanionRepository(private val dao: CompanionDao) {

    val allChapters: Flow<List<ChapterProgress>> = dao.getAllChapterProgress()
    val allClues: Flow<List<Clue>> = dao.getAllClues()
    val allInventory: Flow<List<InventoryItem>> = dao.getAllInventoryItems()

    suspend fun updateChapterStatus(chapterNumber: Int, isCompleted: Boolean, choiceMade: String = "") {
        val completedTime = if (isCompleted) System.currentTimeMillis() else 0L
        dao.updateChapterStatus(chapterNumber, isCompleted, choiceMade, completedTime)

        // Automatically unlock matching story clues or add items as Ethan progresses!
        if (isCompleted) {
            when (chapterNumber) {
                1 -> {
                    // Unlock Chapter 2 clues and find Tower Key
                    dao.updateClueNote("first_transmission", "", System.currentTimeMillis())
                    dao.updateInventoryItemStatus("tower_key", true, System.currentTimeMillis())
                }
                2 -> {
                    // Unlock Chapter 3 clues
                    dao.updateClueNote("journal_day_4", "", System.currentTimeMillis())
                }
                3 -> {
                    // Unlock further journal entries & acquire Ranger's journal
                    dao.updateClueNote("journal_day_7", "", System.currentTimeMillis())
                    dao.updateClueNote("journal_day_10", "", System.currentTimeMillis())
                    dao.updateInventoryItemStatus("ranger_journal", true, System.currentTimeMillis())
                }
                4 -> {
                    // Unlock investigation phase
                }
                5 -> {
                    // Discover bunker keys and tapes
                    dao.updateInventoryItemStatus("bunker_key", true, System.currentTimeMillis())
                    dao.updateInventoryItemStatus("cassette_tapes", true, System.currentTimeMillis())
                }
                6 -> {
                    // Discover classified files and bunker logs
                    dao.updateClueNote("subject_17_file", "", System.currentTimeMillis())
                    dao.updateClueNote("operator_fate", "", System.currentTimeMillis())
                    dao.updateInventoryItemStatus("subject_17_doc", true, System.currentTimeMillis())
                }
            }
        }
    }

    suspend fun saveClueNote(clueId: String, note: String) {
        dao.updateClueNote(clueId, note, System.currentTimeMillis())
    }

    suspend fun addCustomClue(title: String, desc: String, chapter: Int) {
        val customClue = Clue(
            id = "custom_" + System.currentTimeMillis(),
            chapterNumber = chapter,
            title = title,
            description = desc,
            isUnlocked = true,
            userNote = "",
            isCustom = true,
            timestamp = System.currentTimeMillis()
        )
        dao.insertClue(customClue)
    }

    suspend fun deleteClue(clueId: String) {
        dao.deleteClue(clueId)
    }

    suspend fun updateInventoryItemStatus(itemId: String, isFound: Boolean) {
        val time = if (isFound) System.currentTimeMillis() else 0L
        dao.updateInventoryItemStatus(itemId, isFound, time)
    }

    suspend fun resetAllData() {
        dao.resetAllChapters()
        dao.resetAllInventory()
        // Re-seed default clues
        dao.insertAllClues(CompanionDatabase.getPreseededClues())
        // Set Chapter 1 as complete by default as starting point
        dao.updateChapterStatus(1, true, "", System.currentTimeMillis())
        dao.updateInventoryItemStatus("tower_key", true, System.currentTimeMillis())
    }

    /**
     * Packs all progress into a single portable Cloud Transponder backup key (JSON -> Base64)
     */
    fun exportBackupKey(
        chapters: List<ChapterProgress>,
        clues: List<Clue>,
        items: List<InventoryItem>
    ): String {
        return try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())

            // Chapters array
            val chaptersArray = JSONArray()
            chapters.forEach {
                val obj = JSONObject()
                obj.put("num", it.chapterNumber)
                obj.put("comp", it.isCompleted)
                obj.put("choice", it.choiceMade)
                chaptersArray.put(obj)
            }
            root.put("chapters", chaptersArray)

            // Clues array
            val cluesArray = JSONArray()
            clues.forEach {
                val obj = JSONObject()
                obj.put("id", it.id)
                obj.put("unl", it.isUnlocked)
                obj.put("note", it.userNote)
                obj.put("cust", it.isCustom)
                obj.put("title", it.title)
                obj.put("desc", it.description)
                obj.put("chap", it.chapterNumber)
                cluesArray.put(obj)
            }
            root.put("clues", cluesArray)

            // Inventory array
            val itemsArray = JSONArray()
            items.forEach {
                val obj = JSONObject()
                obj.put("id", it.id)
                obj.put("found", it.isFound)
                itemsArray.put(obj)
            }
            root.put("items", itemsArray)

            val jsonString = root.toString()
            val dataBytes = jsonString.toByteArray(StandardCharsets.UTF_8)
            "SIGNAL_SIG_" + Base64.encodeToString(dataBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            "ERROR: Export Failed"
        }
    }

    /**
     * Decodes and restores database state from a portable Cloud Transponder key
     */
    suspend fun importBackupKey(backupKey: String): Boolean {
        try {
            if (!backupKey.startsWith("SIGNAL_SIG_")) return false
            val base64Data = backupKey.substring(11)
            val dataBytes = Base64.decode(base64Data, Base64.NO_WRAP)
            val jsonString = String(dataBytes, StandardCharsets.UTF_8)
            val root = JSONObject(jsonString)

            // Restore Chapters
            val chaptersArray = root.optJSONArray("chapters")
            if (chaptersArray != null) {
                for (i in 0 until chaptersArray.length()) {
                    val obj = chaptersArray.getJSONObject(i)
                    val num = obj.getInt("num")
                    val comp = obj.getBoolean("comp")
                    val choice = obj.optString("choice", "")
                    dao.updateChapterStatus(num, comp, choice, if (comp) System.currentTimeMillis() else 0L)
                }
            }

            // Restore Clues
            val cluesArray = root.optJSONArray("clues")
            if (cluesArray != null) {
                for (i in 0 until cluesArray.length()) {
                    val obj = cluesArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val unl = obj.getBoolean("unl")
                    val note = obj.optString("note", "")
                    val cust = obj.optBoolean("cust", false)

                    if (cust) {
                        // Insert custom clue if not present
                        val title = obj.getString("title")
                        val desc = obj.getString("desc")
                        val chap = obj.getInt("chap")
                        dao.insertClue(
                            Clue(id, chap, title, desc, unl, note, true, System.currentTimeMillis())
                        )
                    } else {
                        dao.updateClueNote(id, note, System.currentTimeMillis())
                        if (unl) {
                            // ensure it is unlocked
                            dao.insertClue(
                                Clue(id, obj.optInt("chap", 1), obj.optString("title", ""), obj.optString("desc", ""), true, note, false, System.currentTimeMillis())
                            )
                        }
                    }
                }
            }

            // Restore Inventory
            val itemsArray = root.optJSONArray("items")
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val found = obj.getBoolean("found")
                    dao.updateInventoryItemStatus(id, found, if (found) System.currentTimeMillis() else 0L)
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
