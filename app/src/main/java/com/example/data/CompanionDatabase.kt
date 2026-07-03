package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ChapterProgress::class, Clue::class, InventoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class CompanionDatabase : RoomDatabase() {
    abstract fun companionDao(): CompanionDao

    companion object {
        @Volatile
        private var INSTANCE: CompanionDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): CompanionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CompanionDatabase::class.java,
                    "last_signal_database"
                )
                .addCallback(CompanionDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        // Pre-seeded Data Providers
        fun getPreseededChapters() = listOf(
            ChapterProgress(1, "Chapter 1 – Arrival", "Heavy rain. Jeep arrives, ranger leaves Ethan the key and whispers: 'If the radio rings after midnight... Don't answer.'", isCompleted = true, completedAt = System.currentTimeMillis()),
            ChapterProgress(2, "Chapter 2 – First Signal", "Explore the tower. At 12:00 AM, the radio crackles. A voice whispers: 'Something is in the forest' and cuts out."),
            ChapterProgress(3, "Chapter 3 – The Missing Ranger", "Discover the previous ranger's blood-soaked journal detailing a descent into paranormal madness."),
            ChapterProgress(4, "Chapter 4 – Strange Things", "Flickering lights, stopped clocks, and heavy footsteps outside. The radio turns on itself whispering: 'Ethan...'"),
            ChapterProgress(5, "Chapter 5 – Investigation", "Search the watchtower for hidden clues, government cassette tapes, radio frequencies, and the secret bunker key."),
            ChapterProgress(6, "Chapter 6 – The Bunker", "Open the basement. Uncover broken radios, Subject 17 experiment files, and hundreds of recursive recordings."),
            ChapterProgress(7, "Chapter 7 – Final Signal", "The radio activates without static. A calm voice says: 'Ethan. You\\'ve been listening. Now... Answer.' Make your choice.")
        )

        fun getPreseededClues() = listOf(
            Clue("ranger_warning", 1, "The Ranger's Rule", "If the radio rings after midnight... Don't answer.", isUnlocked = true),
            Clue("first_transmission", 2, "12:00 AM Call", "'Something is in the forest' was whispered before the signal cut off.", isUnlocked = false),
            Clue("journal_day_4", 3, "Ranger's Journal: Day 4", "'The voices know my name.'", isUnlocked = false),
            Clue("journal_day_7", 3, "Ranger's Journal: Day 7", "'The forest is changing.'", isUnlocked = false),
            Clue("journal_day_10", 3, "Ranger's Journal: Day 10", "'Never answer the last signal.'", isUnlocked = false),
            Clue("subject_17_file", 6, "Subject 17 Experiment", "'SUBJECT 17 - The Signal is alive. Do not respond.'", isUnlocked = false),
            Clue("operator_fate", 6, "Government File", "Every single watchtower operator has disappeared after answering the final transmission.", isUnlocked = false)
        )

        fun getPreseededInventory() = listOf(
            InventoryItem("tower_key", "Watchtower Door Key", "An old rusty key given by the previous lookout ranger. Grants access to the tower.", foundInChapter = 1, isFound = true, foundAt = System.currentTimeMillis()),
            InventoryItem("ranger_journal", "Bloodied Journal", "The journal of the missing operator. The last pages are coated in dark, dry blood.", foundInChapter = 3, isFound = false),
            InventoryItem("cassette_tapes", "Decrypted Audio Tape", "A cassette found hidden behind the transmitter. Emits strange low-frequency pulses.", foundInChapter = 5, isFound = false),
            InventoryItem("bunker_key", "Secret Bunker Key", "A magnetic security keycard hidden under the floorboards. Opens the heavy basement hatch.", foundInChapter = 5, isFound = false),
            InventoryItem("subject_17_doc", "Subject 17 Experiment Dossier", "Marked CLASSIFIED. Details government experiments trying to contain a sentient radio frequency.", foundInChapter = 6, isFound = false)
        )
    }

    private class CompanionDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.companionDao()
                    dao.insertAllChapters(getPreseededChapters())
                    dao.insertAllClues(getPreseededClues())
                    dao.insertAllInventoryItems(getPreseededInventory())
                }
            }
        }
    }
}
