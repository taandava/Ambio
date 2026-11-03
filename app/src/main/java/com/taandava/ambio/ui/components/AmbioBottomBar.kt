package com.taandava.ambio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AmbioBottomBar(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onOpenPresets: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onOpenPresets,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Saved Soundscape Presets" },
            ) {
                Text(text = "📁", fontSize = 22.sp)
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary)
                    .clickable { onTogglePlay() }
                    .semantics { contentDescription = if (isPlaying) "Stop Playback" else "Start Playback" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isPlaying) "⏹️" else "▶️",
                    fontSize = 26.sp,
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "App Appearance and Themes" },
            ) {
                Text(text = "🎨", fontSize = 22.sp)
            }

            IconButton(
                onClick = onOpenInfo,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "About Ambio" },
            ) {
                Text(text = "ℹ️", fontSize = 22.sp)
            }
        }
    }
}
