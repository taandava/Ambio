package com.taandava.ambio.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taandava.ambio.data.model.MixChannel
import com.taandava.ambio.data.model.SoundItem
import com.taandava.ambio.data.repository.MixStore
import com.taandava.ambio.service.PlaybackService
import com.taandava.ambio.ui.components.AmbioBottomBar
import com.taandava.ambio.ui.components.LiveStatusBanner
import com.taandava.ambio.ui.components.MixerChannelRow
import com.taandava.ambio.ui.dialogs.ChannelDetailsDialog
import com.taandava.ambio.ui.dialogs.InfoDialog
import com.taandava.ambio.ui.dialogs.SavedMixesDialog
import com.taandava.ambio.ui.dialogs.SelectSoundDialog
import com.taandava.ambio.ui.dialogs.SettingsDialog
import com.taandava.ambio.ui.theme.AmbioTheme

@Composable
fun AmbioMixApp(
    getService: () -> PlaybackService?,
) {
    val context = LocalContext.current
    val mixStore = remember { MixStore(context) }

    var currentThemeMode by remember { mutableStateOf(mixStore.getThemeMode()) }
    var savedMixes by remember { mutableStateOf(mixStore.loadSavedMixes()) }
    var activeMix by remember { mutableStateOf(mixStore.getActiveMix()) }

    val playbackState by PlaybackService.state.collectAsState()

    var selectedChannelForDetails by remember { mutableStateOf<MixChannel?>(null) }
    var channelForSoundPicker by remember { mutableStateOf<Int?>(null) }
    var showSavedMixesDialog by remember { mutableStateOf(value = false) }
    var showCreateMixPrompt by remember { mutableStateOf(value = false) }
    var showSettingsDialog by remember { mutableStateOf(value = false) }
    var showInfoDialog by remember { mutableStateOf(value = false) }
    var newMixNameInput by remember { mutableStateOf("") }

    /** Applies [transform] to a single channel of the active mix and persists the result. */
    fun updateActiveChannel(channelId: Int, transform: (MixChannel) -> MixChannel) {
        val current = activeMix ?: return
        val updatedChannels = current.channels.map { ch ->
            if (ch.channelId == channelId) transform(ch) else ch
        }
        val updatedMix = current.copy(channels = updatedChannels)
        activeMix = updatedMix
        mixStore.saveMix(updatedMix)
    }

    val openAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {
            }

            val fileName = queryDisplayName(context, uri) ?: "Device Sound"
            val newCustomSound = SoundItem(
                id = uri.toString(),
                name = fileName,
                uriString = uri.toString(),
            )
            mixStore.saveCustomSound(newCustomSound)

            channelForSoundPicker?.let { chId ->
                activeMix?.let { curMix ->
                    val updatedChannels = curMix.channels.map {
                        if (it.channelId == chId) {
                            it.copy(sound = newCustomSound, customName = null, isMuted = false)
                        } else {
                            it
                        }
                    }
                    val updatedMix = curMix.copy(channels = updatedChannels)
                    activeMix = updatedMix
                    mixStore.saveMix(updatedMix)
                    getService()?.setChannelSound(chId, newCustomSound)
                }
                channelForSoundPicker = null
            }
        }
    }

    AmbioTheme(themeMode = currentThemeMode) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AmbioBottomBar(
                    isPlaying = playbackState.isPlaying,
                    onTogglePlay = {
                        val service = getService()
                        val currentMix = activeMix
                        if (playbackState.isPlaying) {
                            service?.stopPlayback()
                        } else if (currentMix != null && currentMix.channels.any { ch ->
                                (ch.sound != null) && (!ch.isMuted)
                            }
                        ) {
                            service?.setMix(currentMix, autoPlay = true)
                            service?.startPlayback()
                        } else {
                            if (savedMixes.isEmpty() || activeMix == null) {
                                newMixNameInput = "My Mix 1"
                                showCreateMixPrompt = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "Select at least one sound first",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    onOpenPresets = { showSavedMixesDialog = true },
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenInfo = { showInfoDialog = true },
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopStart,
            ) {
                val currentMix = activeMix
                if (savedMixes.isNotEmpty() && currentMix != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp)
                            .windowInsetsPadding(WindowInsets.statusBars),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        LiveStatusBanner(
                            isPlaying = playbackState.isPlaying,
                            activeChannelsCount = currentMix.channels.count { ch ->
                                (ch.sound != null) && (!ch.isMuted)
                            },
                            masterVolume = playbackState.masterVolume,
                            onMasterVolumeChanged = { vol ->
                                getService()?.setMasterVolume(vol)
                            },
                            sleepTimerRemainingSeconds = playbackState.sleepTimerRemainingSeconds,
                            onStartSleepTimer = { mins ->
                                getService()?.startSleepTimer(mins)
                            },
                            onCancelSleepTimer = {
                                getService()?.cancelSleepTimer()
                            },
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = currentMix.channels,
                                key = { it.channelId },
                            ) { ch ->
                                MixerChannelRow(
                                    channel = ch,
                                    onToggleMute = {
                                        getService()?.toggleChannelMute(ch.channelId)?.let { muted ->
                                            updateActiveChannel(ch.channelId) { it.copy(isMuted = muted) }
                                        }
                                    },
                                    onVolumeChanged = { vol ->
                                        getService()?.updateChannelVolume(ch.channelId, vol)
                                        updateActiveChannel(ch.channelId) { it.copy(volume = vol) }
                                    },
                                    onEmptySlotClicked = {
                                        channelForSoundPicker = ch.channelId
                                        selectedChannelForDetails = null
                                    },
                                    onChannelDetailsClicked = {
                                        selectedChannelForDetails = ch
                                    },
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                } else {
                    BlankHomeScreen(
                        onCreateMixClicked = {
                            newMixNameInput = "My Mix 1"
                            showCreateMixPrompt = true
                        },
                    )
                }
            }
        }

        selectedChannelForDetails?.let { ch ->
            ChannelDetailsDialog(
                channel = ch,
                onChangeSound = {
                    channelForSoundPicker = ch.channelId
                    selectedChannelForDetails = null
                },
                onSave = { customName, repeatSec, isFuzzy, vol, pitch, speed ->
                    activeMix?.let { curMix ->
                        val updatedChannels = curMix.channels.map { existing ->
                            if (existing.channelId == ch.channelId) {
                                existing.copy(
                                    customName = customName,
                                    volume = vol,
                                    repeatIntervalSec = repeatSec,
                                    isFuzzyRepeat = isFuzzy,
                                    pitch = pitch,
                                    speed = speed,
                                )
                            } else {
                                existing
                            }
                        }
                        val updatedMix = curMix.copy(channels = updatedChannels)
                        activeMix = updatedMix
                        mixStore.saveMix(updatedMix)
                        getService()?.updateChannelDetails(
                            ch.channelId,
                            customName,
                            repeatSec,
                            isFuzzy,
                            vol,
                            pitch,
                            speed,
                        )
                    }
                    selectedChannelForDetails = null
                },
                onDelete = {
                    activeMix?.let { curMix ->
                        val updatedChannels = curMix.channels.map { existing ->
                            if (existing.channelId == ch.channelId) {
                                MixChannel(channelId = ch.channelId)
                            } else {
                                existing
                            }
                        }
                        val updatedMix = curMix.copy(channels = updatedChannels)
                        activeMix = updatedMix
                        mixStore.saveMix(updatedMix)
                        getService()?.removeChannel(ch.channelId)
                    }
                    selectedChannelForDetails = null
                },
                onDismiss = {
                    selectedChannelForDetails = null
                },
            )
        }

        channelForSoundPicker?.let { chId ->
            SelectSoundDialog(
                customSounds = mixStore.loadCustomSounds(),
                onSelectSound = { soundItem ->
                    activeMix?.let { curMix ->
                        val updatedChannels = curMix.channels.map { existing ->
                            if (existing.channelId == chId) {
                                existing.copy(
                                    sound = soundItem,
                                    customName = null,
                                    isMuted = false,
                                )
                            } else {
                                existing
                            }
                        }
                        val updatedMix = curMix.copy(channels = updatedChannels)
                        activeMix = updatedMix
                        mixStore.saveMix(updatedMix)
                        getService()?.setChannelSound(chId, soundItem)
                    }
                    channelForSoundPicker = null
                },
                onPickFromSystemPicker = {
                    openAudioLauncher.launch(arrayOf("audio/*"))
                },
                onDeleteCustomSound = { id ->
                    mixStore.deleteCustomSound(id)
                },
                onDismiss = {
                    channelForSoundPicker = null
                },
            )
        }

        if (showSavedMixesDialog) {
            SavedMixesDialog(
                mixStore = mixStore,
                activeMixId = activeMix?.id,
                onLoadMix = { selectedMix ->
                    activeMix = selectedMix
                    mixStore.setActiveMixId(selectedMix.id)
                    getService()?.setMix(selectedMix)
                    showSavedMixesDialog = false
                    Toast.makeText(
                        context,
                        "Loaded soundscape: ${selectedMix.name}",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onCreateNew = {
                    showSavedMixesDialog = false
                    newMixNameInput = "Mix ${savedMixes.size + 1}"
                    showCreateMixPrompt = true
                },
                onMixesChanged = {
                    savedMixes = mixStore.loadSavedMixes()
                    activeMix = mixStore.getActiveMix()
                },
                onDismiss = {
                    showSavedMixesDialog = false
                },
            )
        }

        if (showCreateMixPrompt) {
            AlertDialog(
                onDismissRequest = { showCreateMixPrompt = false },
                confirmButton = {
                    Button(
                        onClick = {
                            val name = newMixNameInput.trim().ifBlank {
                                "Mix ${savedMixes.size + 1}"
                            }
                            val created = mixStore.createNewMix(name)
                            savedMixes = mixStore.loadSavedMixes()
                            activeMix = created
                            getService()?.setMix(created)
                            showCreateMixPrompt = false
                            Toast.makeText(
                                context,
                                "Created '$name'",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateMixPrompt = false }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Create New Mix",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Enter a title for your custom soundscape:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newMixNameInput,
                            onValueChange = { newMixNameInput = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                currentThemeMode = currentThemeMode,
                onThemeModeSelected = { mode ->
                    currentThemeMode = mode
                    mixStore.setThemeMode(mode)
                },
                onDismiss = { showSettingsDialog = false },
            )
        }

        if (showInfoDialog) {
            InfoDialog(onDismiss = { showInfoDialog = false })
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(
            uri,
            arrayOf("_display_name"),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex("_display_name")
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}
