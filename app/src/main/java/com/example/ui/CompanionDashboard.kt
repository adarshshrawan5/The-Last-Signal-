package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChapterProgress
import com.example.data.Clue
import com.example.data.InventoryItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionDashboard(viewModel: CompanionViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Core state links
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val clues by viewModel.clues.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    
    val selectedChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingSignal.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val backupKeyExported by viewModel.backupKeyExported.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    
    // Config states
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val ambientVolume by viewModel.ambientVolume.collectAsStateWithLifecycle()
    val staticVolume by viewModel.staticVolume.collectAsStateWithLifecycle()
    val tunedFrequency by viewModel.tunedFrequency.collectAsStateWithLifecycle()
    
    // Local UI states
    var activeTab by remember { mutableStateOf(0) } // 0 = Chapters, 1 = Inventory, 2 = Clues, 3 = Receiver Settings
    var showCustomClueDialog by remember { mutableStateOf(false) }
    var redLightPulse by remember { mutableStateOf(true) }
    
    // Flashing "recording/receiver" indicator thread
    LaunchedEffect(Unit) {
        while (true) {
            redLightPulse = !redLightPulse
            delay(1000)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PitchBlack,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkObsidian,
                    titleContentColor = BloodRed
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Flashing Warning receiver light
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (redLightPulse) BloodRed else DarkCrimson)
                                .border(1.dp, Color.Black, RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "THE LAST SIGNAL",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = DarkCrimson.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, BloodRed)
                        ) {
                            Text(
                                text = "STATION 4",
                                color = TerminalAmber,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkObsidian,
                tonalElevation = 8.dp,
                modifier = Modifier.border(BorderStroke(1.dp, DeepSlateGrey))
            ) {
                val tabs = listOf(
                    Triple("LOGS", Icons.Default.MenuBook, 0),
                    Triple("ITEMS", Icons.Default.BusinessCenter, 1),
                    Triple("TERMINAL", Icons.Default.Terminal, 2),
                    Triple("SYNC", Icons.Default.CellTower, 3)
                )
                tabs.forEach { (label, icon, index) ->
                    val isSelected = activeTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            activeTab = index
                            viewModel.playSoundChirp(highPitch = true)
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) BloodRed else StaticGrey,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BloodRed else StaticGrey
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MediumSlateGrey
                        ),
                        modifier = Modifier.testTag("tab_$label")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PitchBlack)
        ) {
            // Eerie Broadcast Banner
            Surface(
                color = DarkObsidian,
                border = BorderStroke(1.dp, DeepSlateGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Radio status",
                        tint = TerminalAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOOKOUT ETHAN CARTER • FREQ: ${"%.1f".format(tunedFrequency)} MHz • OUTSIDE TEMP: 6°C",
                        color = TerminalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> ChaptersScreen(
                        chapters = chapters,
                        selectedChapter = selectedChapter,
                        onSelectChapter = { viewModel.selectChapter(it) },
                        onToggleComplete = { chNum, complete, choice ->
                            viewModel.toggleChapterCompletion(chNum, complete, choice)
                        }
                    )
                    1 -> InventoryScreen(
                        items = inventory,
                        onToggleItemFound = { id, found ->
                            viewModel.toggleInventoryItem(id, found)
                        }
                    )
                    2 -> CluesScreen(
                        clues = clues,
                        onSaveNote = { id, text -> viewModel.saveClueNote(id, text) },
                        onAddCustomClueClick = { showCustomClueDialog = true },
                        onDeleteClue = { id -> viewModel.deleteClue(id) },
                        isAnalyzing = isAnalyzing,
                        analysisResult = analysisResult,
                        onAnalyzeClick = { text -> viewModel.analyzeClueWithGemini(text) },
                        onClearAnalysis = { viewModel.clearAnalysis() }
                    )
                    3 -> ReceiverSettingsScreen(
                        ambientVolume = ambientVolume,
                        staticVolume = staticVolume,
                        tunedFrequency = tunedFrequency,
                        geminiApiKey = geminiApiKey,
                        syncStatusMessage = syncStatusMessage,
                        backupKeyExported = backupKeyExported,
                        onAmbientVolChanged = { viewModel.updateAmbientVolume(it) },
                        onStaticVolChanged = { viewModel.updateStaticVolume(it) },
                        onFreqChanged = { viewModel.updateTunedFrequency(it) },
                        onApiKeyChanged = { viewModel.setGeminiApiKey(it) },
                        onExportBackup = { viewModel.generateCloudBackupCode() },
                        onImportBackup = { viewModel.restoreCloudBackup(it) },
                        onClearSyncMsg = { viewModel.clearSyncStatus() },
                        onResetProgress = { viewModel.resetAllProgress() }
                    )
                }
            }
        }
    }

    // Custom Clue Creator Dialog
    if (showCustomClueDialog) {
        CustomClueDialog(
            onDismiss = { showCustomClueDialog = false },
            onSave = { title, desc, chap ->
                viewModel.addCustomClue(title, desc, chap)
                showCustomClueDialog = false
            }
        )
    }
}

// ==================== CHAPTERS LOGS SCREEN ====================

@Composable
fun ChaptersScreen(
    chapters: List<ChapterProgress>,
    selectedChapter: ChapterProgress?,
    onSelectChapter: (ChapterProgress?) -> Unit,
    onToggleComplete: (Int, Boolean, String) -> Unit
) {
    if (selectedChapter != null) {
        ChapterDetailDialog(
            chapter = selectedChapter,
            onDismiss = { onSelectChapter(null) },
            onToggleComplete = { comp, choice ->
                onToggleComplete(selectedChapter.chapterNumber, comp, choice)
                onSelectChapter(null)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CASE FILE: RECENT CHRONICLES",
                color = BloodRed,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(chapters) { chapter ->
            val isCompleted = chapter.isCompleted
            Surface(
                color = if (isCompleted) DeepSlateGrey else DarkObsidian,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isCompleted) BloodRed else DeepSlateGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectChapter(chapter) }
                    .testTag("chapter_card_${chapter.chapterNumber}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.title,
                            color = if (isCompleted) GhostlyWhite else StaticGrey,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chapter.description,
                            color = if (isCompleted) StaticGrey else StaticGrey.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Completion status indicator
                    if (isCompleted) {
                        Column(horizontalAlignment = Alignment.End) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Chapter Completed",
                                tint = TerminalGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            if (chapter.choiceMade.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = chapter.choiceMade.uppercase(),
                                    color = TerminalAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Chapter Locked",
                            tint = StaticGrey,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ChapterDetailDialog(
    chapter: ChapterProgress,
    onDismiss: () -> Unit,
    onToggleComplete: (Boolean, String) -> Unit
) {
    var endingChoice by remember { mutableStateOf(chapter.choiceMade) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkObsidian,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BloodRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = chapter.title,
                    color = BloodRed,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = DeepSlateGrey)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = chapter.description,
                    color = GhostlyWhite,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Special ending triggers for Chapter 7 (The Final Choice)
                if (chapter.chapterNumber == 7) {
                    Text(
                        text = "CHOOSE ETHAN'S FATE:",
                        color = TerminalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { endingChoice = "Destroy Radio" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (endingChoice == "Destroy Radio") BloodRed else MediumSlateGrey
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_destroy_radio"),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "DESTROY RADIO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Button(
                            onClick = { endingChoice = "Answer Signal" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (endingChoice == "Answer Signal") DarkCrimson else MediumSlateGrey
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_answer_signal"),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ANSWER SIGNAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Expose decision lore immediately
                    if (endingChoice == "Destroy Radio") {
                        Surface(
                            color = PitchBlack,
                            border = BorderStroke(1.dp, TerminalGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ENDING 1 (GOOD): Ethan smashes the radio to pieces. A powerful energy shockwave destroys the radio waves. Helis find Ethan alive by morning. But as they fly off... a tiny red light blinks on the broken dashboard.",
                                color = TerminalGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else if (endingChoice == "Answer Signal") {
                        Surface(
                            color = PitchBlack,
                            border = BorderStroke(1.dp, BloodRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ENDING 2 (BAD): Ethan grips the transmitter: 'I'm here.' Blackout. Years later, a new lookup operator arrives. The radio crackles, playing a familiar voice: 'Can anyone hear me?' Ethan has become the signal.",
                                color = FadedRed,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = StaticGrey, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (chapter.chapterNumber == 7 && endingChoice.isEmpty()) {
                                // Default option
                                onToggleComplete(true, "Undecided")
                            } else {
                                onToggleComplete(!chapter.isCompleted, endingChoice)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (chapter.isCompleted) "RESET LOG" else "MARK COMPLETED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==================== INVENTORY LOG SCREEN ====================

@Composable
fun InventoryScreen(
    items: List<InventoryItem>,
    onToggleItemFound: (String, Boolean) -> Unit
) {
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }

    if (selectedItem != null) {
        ItemDetailDialog(
            item = selectedItem!!,
            onDismiss = { selectedItem = null },
            onToggleFound = { found ->
                onToggleItemFound(selectedItem!!.id, found)
                selectedItem = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "EQUIPMENT LOG & FINDINGS",
            color = BloodRed,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                val isFound = item.isFound
                Surface(
                    color = if (isFound) DeepSlateGrey else PitchBlack,
                    border = BorderStroke(1.dp, if (isFound) BloodRed else DeepSlateGrey),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable { selectedItem = item }
                        .testTag("item_card_${item.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isFound) Icons.Default.Inventory else Icons.Default.Lock,
                                contentDescription = item.name,
                                tint = if (isFound) TerminalAmber else StaticGrey,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CH ${item.foundInChapter}",
                                color = if (isFound) TerminalAmber else StaticGrey,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = item.name,
                            color = if (isFound) GhostlyWhite else StaticGrey,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (isFound) "IN POSSESSION" else "UNDISCOVERED",
                            color = if (isFound) TerminalGreen else StaticGrey.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDetailDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onToggleFound: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkObsidian,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BloodRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isFound) Icons.Default.Inventory else Icons.Default.Lock,
                        contentDescription = item.name,
                        tint = if (item.isFound) TerminalAmber else StaticGrey,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        color = BloodRed,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = DeepSlateGrey)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "LORE DATA:",
                    color = TerminalAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    color = GhostlyWhite,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DISCOVERY LOCATION: Chapter ${item.foundInChapter}",
                    color = StaticGrey,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("BACK", color = StaticGrey, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onToggleFound(!item.isFound) },
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("btn_toggle_item")
                    ) {
                        Text(
                            text = if (item.isFound) "LOSE ITEM" else "CLAIM ITEM",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==================== CLUES & NOTES SCREEN ====================

@Composable
fun CluesScreen(
    clues: List<Clue>,
    onSaveNote: (String, String) -> Unit,
    onAddCustomClueClick: () -> Unit,
    onDeleteClue: (String) -> Unit,
    isAnalyzing: Boolean,
    analysisResult: String,
    onAnalyzeClick: (String) -> Unit,
    onClearAnalysis: () -> Unit
) {
    var activeClueForNotes by remember { mutableStateOf<Clue?>(null) }
    var activeNoteText by remember { mutableStateOf("") }
    
    // Auto-update note field if active clue changes
    LaunchedEffect(activeClueForNotes) {
        activeNoteText = activeClueForNotes?.userNote ?: ""
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // AI Signal decrypter terminal header
        item {
            Text(
                text = "COGNITIVE SIGNAL DECRYPTER",
                color = BloodRed,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            // Retro Terminal box for Gemini
            Surface(
                color = PitchBlack,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, TerminalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isAnalyzing) TerminalAmber else TerminalGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "COSMIC TRANSCEIVER ONLINE",
                                color = TerminalGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (analysisResult.isNotEmpty()) {
                            Text(
                                text = "[CLEAR]",
                                color = BloodRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clickable { onClearAnalysis() }
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = analysisResult.ifEmpty { "Select any clue details below and transmit to the cloud radio waves to decrypt the hidden message from the entity." },
                        color = if (analysisResult.contains("WARNING")) TerminalAmber else GhostlyWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Action controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CLUES & NOTES LOG",
                    color = TerminalAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Button(
                    onClick = onAddCustomClueClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_add_custom_clue")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add log",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "ADD CUSTOM CLUE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Clue listing
        val unlockedClues = clues.filter { it.isUnlocked }
        if (unlockedClues.isEmpty()) {
            item {
                Surface(
                    color = DarkObsidian,
                    border = BorderStroke(1.dp, DeepSlateGrey),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "THE RECEPTACLE IS EMPTY.\nComplete chapters to unlock pre-seeded story clues, or tap 'Add Custom Clue' above to write notes manually.",
                        color = StaticGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(unlockedClues) { clue ->
                Surface(
                    color = DarkObsidian,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (activeClueForNotes?.id == clue.id) TerminalAmber else DeepSlateGrey),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clue.title,
                                    color = GhostlyWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Origin: Chapter ${clue.chapterNumber}",
                                    color = StaticGrey,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            // Delete button for custom clues
                            if (clue.isCustom) {
                                IconButton(
                                    onClick = { onDeleteClue(clue.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete custom note",
                                        tint = BloodRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = clue.description,
                            color = StaticGrey,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // User note terminal edit box
                        if (activeClueForNotes?.id == clue.id) {
                            OutlinedTextField(
                                value = activeNoteText,
                                onValueChange = { activeNoteText = it },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TerminalGreen
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TerminalGreen,
                                    unfocusedBorderColor = StaticGrey,
                                    focusedContainerColor = PitchBlack,
                                    unfocusedContainerColor = PitchBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .testTag("input_note_text"),
                                placeholder = {
                                    Text(
                                        "Input decryption logs, frequencies or coordinates...",
                                        fontSize = 11.sp,
                                        color = StaticGrey.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { onAnalyzeClick(clue.title + ": " + clue.description + " Notes: " + activeNoteText) },
                                    enabled = !isAnalyzing,
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkCrimson),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.testTag("btn_analyze_clue")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CellTower,
                                        contentDescription = "Analyze",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "DECRYPT SIGNAL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row {
                                    TextButton(onClick = { activeClueForNotes = null }) {
                                        Text("CLOSE", color = StaticGrey, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            onSaveNote(clue.id, activeNoteText)
                                            activeClueForNotes = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("SAVE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Expand/write button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (clue.userNote.isEmpty()) "NO NOTES LOGGED" else "LOGGED NOTES: ${clue.userNote}",
                                    color = if (clue.userNote.isEmpty()) StaticGrey.copy(alpha = 0.5f) else TerminalGreen,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MediumSlateGrey,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .clickable { activeClueForNotes = clue }
                                        .testTag("btn_write_note_${clue.id}")
                                ) {
                                    Text(
                                        text = "WRITE LOG",
                                        color = GhostlyWhite,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CustomClueDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkObsidian,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BloodRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MANUAL LOG TERMINAL",
                    color = BloodRed,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = DeepSlateGrey)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Clue Title", fontFamily = FontFamily.Monospace) },
                    textStyle = TextStyle(color = GhostlyWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BloodRed,
                        unfocusedBorderColor = StaticGrey,
                        focusedLabelColor = BloodRed,
                        unfocusedLabelColor = StaticGrey
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_clue_title")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Clue Description / Warning", fontFamily = FontFamily.Monospace) },
                    textStyle = TextStyle(color = GhostlyWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BloodRed,
                        unfocusedBorderColor = StaticGrey,
                        focusedLabelColor = BloodRed,
                        unfocusedLabelColor = StaticGrey
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("input_custom_clue_desc")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chapter selection slider/selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CHAPTER: $chapter",
                        color = TerminalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(100.dp)
                    )
                    Slider(
                        value = chapter.toFloat(),
                        onValueChange = { chapter = it.toInt() },
                        valueRange = 1f..7f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = BloodRed,
                            activeTrackColor = BloodRed,
                            inactiveTrackColor = DeepSlateGrey
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = StaticGrey, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && description.isNotEmpty()) {
                                onSave(title, description, chapter)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("btn_save_custom_clue")
                    ) {
                        Text("LOG CLUE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== RECEIVER & CLOUD SYNC SCREEN ====================

@Composable
fun ReceiverSettingsScreen(
    ambientVolume: Float,
    staticVolume: Float,
    tunedFrequency: Float,
    geminiApiKey: String,
    syncStatusMessage: String,
    backupKeyExported: String,
    onAmbientVolChanged: (Float) -> Unit,
    onStaticVolChanged: (Float) -> Unit,
    onFreqChanged: (Float) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (String) -> Unit,
    onClearSyncMsg: () -> Unit,
    onResetProgress: () -> Unit
) {
    val context = LocalContext.current
    var inputBackupCode by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var isSimulatingSync by remember { mutableStateOf(false) }
    var simulatedProgress by remember { mutableStateOf(0.0f) }
    val scope = rememberCoroutineScope()

    // Trigger Clipboard copying if backup key changes
    LaunchedEffect(backupKeyExported) {
        if (backupKeyExported.isNotEmpty() && backupKeyExported.startsWith("SIGNAL_SIG_")) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("LastSignalBackup", backupKeyExported)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Transponder key copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- SECTION 1: FREQUENCY TUNER & SOUND GENERATOR ---
        item {
            Text(
                text = "RADIO RECEIVER CONTROLLER",
                color = BloodRed,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Surface(
                color = DarkObsidian,
                border = BorderStroke(1.dp, DeepSlateGrey),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Frequency tuner slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAL FREQUENCY",
                            color = GhostlyWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${"%.1f".format(tunedFrequency)} MHz",
                            color = TerminalAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = tunedFrequency,
                        onValueChange = onFreqChanged,
                        valueRange = 88.0f..108.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = BloodRed,
                            activeTrackColor = BloodRed,
                            inactiveTrackColor = DeepSlateGrey
                        ),
                        modifier = Modifier.testTag("slider_frequency")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Drone volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DREAD DRONE VOLUME",
                            color = StaticGrey,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(ambientVolume * 100).toInt()}%",
                            color = TerminalAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Slider(
                        value = ambientVolume,
                        onValueChange = onAmbientVolChanged,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = BloodRed,
                            activeTrackColor = BloodRed,
                            inactiveTrackColor = DeepSlateGrey
                        ),
                        modifier = Modifier.testTag("slider_drone")
                    )

                    // Static Volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STATIC STATIC VOLUME",
                            color = StaticGrey,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(staticVolume * 100).toInt()}%",
                            color = TerminalAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Slider(
                        value = staticVolume,
                        onValueChange = onStaticVolChanged,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = BloodRed,
                            activeTrackColor = BloodRed,
                            inactiveTrackColor = DeepSlateGrey
                        ),
                        modifier = Modifier.testTag("slider_static")
                    )
                }
            }
        }

        // --- SECTION 2: QUANTUM SAVE TRANSPONDER (CLOUD SYNC) ---
        item {
            Text(
                text = "QUANTUM SAVE TRANSPONDER",
                color = BloodRed,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Surface(
                color = DarkObsidian,
                border = BorderStroke(1.dp, DeepSlateGrey),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Encodes full player inventory, chapters completion, and hand-written logs into zero-server Base64 transponder keys.",
                        color = StaticGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (syncStatusMessage.isNotEmpty()) {
                        Surface(
                            color = PitchBlack,
                            border = BorderStroke(1.dp, TerminalGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClearSyncMsg() }
                        ) {
                            Text(
                                text = syncStatusMessage,
                                color = TerminalGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExportBackup,
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_sync")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Export key")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT KEY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (!isSimulatingSync) {
                                    isSimulatingSync = true
                                    scope.launch {
                                        simulatedProgress = 0.0f
                                        while (simulatedProgress < 1.0f) {
                                            delay(150)
                                            simulatedProgress += 0.1f
                                        }
                                        isSimulatingSync = false
                                        onExportBackup() // Trigger real export and say success
                                        Toast.makeText(context, "SATELLITE SYNC TRANSCEIVER LINK SECURED!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCrimson),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("btn_cloud_sync")
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Cloud Save")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isSimulatingSync) "SYNCING..." else "CLOUD SAVE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isSimulatingSync) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { simulatedProgress },
                            color = TerminalGreen,
                            trackColor = DeepSlateGrey,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputBackupCode,
                        onValueChange = { inputBackupCode = it },
                        label = { Text("Paste Transponder Key to Restore", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalAmber),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalAmber,
                            unfocusedBorderColor = StaticGrey,
                            focusedLabelColor = TerminalAmber,
                            unfocusedLabelColor = StaticGrey
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_restore_sync"),
                        trailingIcon = {
                            if (inputBackupCode.isNotEmpty()) {
                                IconButton(onClick = {
                                    onImportBackup(inputBackupCode)
                                    inputBackupCode = ""
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Restore",
                                        tint = TerminalAmber
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // --- SECTION 3: API TERMINAL CREDENTIALS ---
        item {
            Text(
                text = "CRYPTO TRANSMISSION CREDENTIALS",
                color = BloodRed,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Surface(
                color = DarkObsidian,
                border = BorderStroke(1.dp, DeepSlateGrey),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Configures your private Gemini API Key. It remains strictly offline in local SharedPreferences and is used to decrypt radio signals.",
                        color = StaticGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = onApiKeyChanged,
                        label = { Text("LOCAL API SIGNAL KEY", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TerminalGreen),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalGreen,
                            unfocusedBorderColor = StaticGrey,
                            focusedLabelColor = TerminalGreen,
                            unfocusedLabelColor = StaticGrey
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_api_key"),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Key View",
                                    tint = StaticGrey
                                )
                            }
                        }
                    )
                }
            }
        }

        // --- SECTION 4: DESTRUCTIVE CONTROLS ---
        item {
            Surface(
                color = DarkObsidian,
                border = BorderStroke(1.dp, DarkCrimson),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DANGER ZONE",
                        color = BloodRed,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wipes Ethan's database completely and resets back to Day 1 arrival logs.",
                        color = StaticGrey,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onResetProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_reset_all")
                    ) {
                        Text(
                            "ERASE SIGNAL DATA (RESET)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
