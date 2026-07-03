package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SignalSynthesizer
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CompanionViewModel(application: Application) : AndroidViewModel(application) {

    private val dbScope = viewModelScope
    private val database = CompanionDatabase.getDatabase(application, dbScope)
    private val repository = CompanionRepository(database.companionDao())

    // UI State Flows
    val chapters: StateFlow<List<ChapterProgress>> = repository.allChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clues: StateFlow<List<Clue>> = repository.allClues
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventory: StateFlow<List<InventoryItem>> = repository.allInventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audio Synthesizer
    val synthesizer = SignalSynthesizer()

    // Shared Preferences for local settings (persistent API key, sound settings)
    private val prefs = application.getSharedPreferences("last_signal_prefs", Context.MODE_PRIVATE)

    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _ambientVolume = MutableStateFlow(prefs.getFloat("ambient_vol", 0.3f))
    val ambientVolume: StateFlow<Float> = _ambientVolume.asStateFlow()

    private val _staticVolume = MutableStateFlow(prefs.getFloat("static_vol", 0.15f))
    val staticVolume: StateFlow<Float> = _staticVolume.asStateFlow()

    private val _tunedFrequency = MutableStateFlow(prefs.getFloat("tuned_freq", 100.0f))
    val tunedFrequency: StateFlow<Float> = _tunedFrequency.asStateFlow()

    // Interactive UI states
    private val _selectedChapter = MutableStateFlow<ChapterProgress?>(null)
    val selectedChapter: StateFlow<ChapterProgress?> = _selectedChapter.asStateFlow()

    private val _isAnalyzingSignal = MutableStateFlow(false)
    val isAnalyzingSignal: StateFlow<Boolean> = _isAnalyzingSignal.asStateFlow()

    private val _analysisResult = MutableStateFlow("")
    val analysisResult: StateFlow<String> = _analysisResult.asStateFlow()

    private val _backupKeyExported = MutableStateFlow("")
    val backupKeyExported: StateFlow<String> = _backupKeyExported.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    init {
        // Apply initial audio settings
        synthesizer.droneVolume = _ambientVolume.value
        synthesizer.staticVolume = _staticVolume.value
        synthesizer.signalFrequency = _tunedFrequency.value
        
        // Start atmospheric background noise automatically if active
        synthesizer.startAmbientDrone()
    }

    // --- Audio Control Functions ---
    fun updateAmbientVolume(volume: Float) {
        _ambientVolume.value = volume
        synthesizer.droneVolume = volume
        prefs.edit().putFloat("ambient_vol", volume).apply()
    }

    fun updateStaticVolume(volume: Float) {
        _staticVolume.value = volume
        synthesizer.staticVolume = volume
        prefs.edit().putFloat("static_vol", volume).apply()
    }

    fun updateTunedFrequency(freq: Float) {
        _tunedFrequency.value = freq
        synthesizer.signalFrequency = freq
        synthesizer.pitchSweep = (freq - 100.0f) * 0.5f // Modulate drone pitch based on frequency slider!
        prefs.edit().putFloat("tuned_freq", freq).apply()
    }

    fun playSoundChirp(highPitch: Boolean) {
        synthesizer.playChirp(highPitch)
    }

    fun transmitMorse(word: String) {
        synthesizer.playMorseCode(word)
    }

    // --- State & Operations ---
    fun selectChapter(chapter: ChapterProgress?) {
        _selectedChapter.value = chapter
    }

    fun setGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        prefs.edit().putString("gemini_key", key).apply()
    }

    fun toggleChapterCompletion(chapterNumber: Int, isCompleted: Boolean, choice: String = "") {
        viewModelScope.launch {
            repository.updateChapterStatus(chapterNumber, isCompleted, choice)
            // Play static frequency burst on progression
            playSoundChirp(highPitch = false)
            transmitMorse("E") // Morse notification beep for Ethan
        }
    }

    fun saveClueNote(clueId: String, note: String) {
        viewModelScope.launch {
            repository.saveClueNote(clueId, note)
        }
    }

    fun addCustomClue(title: String, desc: String, chapter: Int) {
        viewModelScope.launch {
            repository.addCustomClue(title, desc, chapter)
            playSoundChirp(highPitch = true)
        }
    }

    fun deleteClue(clueId: String) {
        viewModelScope.launch {
            repository.deleteClue(clueId)
            playSoundChirp(highPitch = false)
        }
    }

    fun toggleInventoryItem(itemId: String, isFound: Boolean) {
        viewModelScope.launch {
            repository.updateInventoryItemStatus(itemId, isFound)
            playSoundChirp(highPitch = isFound)
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repository.resetAllData()
            _selectedChapter.value = null
            _analysisResult.value = ""
            transmitMorse("SOS")
        }
    }

    // --- Cloud Sync Backup & Restore ---
    fun generateCloudBackupCode() {
        viewModelScope.launch {
            val key = repository.exportBackupKey(
                chapters.value,
                clues.value,
                inventory.value
            )
            _backupKeyExported.value = key
            _syncStatusMessage.value = "QUANTUM KEY LOADED. Copied to buffer."
        }
    }

    fun restoreCloudBackup(key: String) {
        viewModelScope.launch {
            val success = repository.importBackupKey(key.trim())
            if (success) {
                _syncStatusMessage.value = "SYNC COMPLETED. Data transponder synchronized."
                playSoundChirp(highPitch = true)
            } else {
                _syncStatusMessage.value = "ERROR: INTERFERENCE. Corrupt backup key."
                playSoundChirp(highPitch = false)
            }
        }
    }

    fun clearSyncStatus() {
        _syncStatusMessage.value = ""
        _backupKeyExported.value = ""
    }

    // --- Gemini Atmospheric Analysis ---
    fun analyzeClueWithGemini(clueText: String) {
        if (clueText.isBlank()) return
        viewModelScope.launch {
            _isAnalyzingSignal.value = true
            _analysisResult.value = "[DECRYPTING TRANSMISSION FREQUENCY...]"
            playSoundChirp(highPitch = false)
            
            val response = GeminiClient.analyzeSignal(clueText, _geminiApiKey.value)
            _analysisResult.value = response
            _isAnalyzingSignal.value = false
            
            // Play Morse signaling when analysis completes successfully!
            if (!response.contains("WARNING") && !response.contains("failed")) {
                transmitMorse("T")
            }
        }
    }

    fun clearAnalysis() {
        _analysisResult.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        synthesizer.release()
    }
}
