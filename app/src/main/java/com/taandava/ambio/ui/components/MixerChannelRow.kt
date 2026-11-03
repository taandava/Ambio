package com.taandava.ambio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taandava.ambio.data.model.MixChannel

@Composable
fun MixerChannelRow(
    channel: MixChannel,
    onToggleMute: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onEmptySlotClicked: () -> Unit,
    onChannelDetailsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFilled = channel.sound != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFilled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        ),
        border = if (isFilled && !channel.isMuted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Channel Action/Icon Box (Mute/Unmute or Add)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            !isFilled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            channel.isMuted -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        },
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            !isFilled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            channel.isMuted -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable {
                        if (isFilled) onToggleMute() else onEmptySlotClicked()
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isFilled) {
                    if (channel.isMuted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = "Muted",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Playing",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "ON",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AddCircleOutline,
                        contentDescription = "Add Sound Track",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isFilled) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = channel.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (channel.isMuted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if ((channel.pitch != 1.0f) || (channel.speed != 1.0f)) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = "Pitch/Speed Modulated",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 2.dp),
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (channel.isMuted) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                if (channel.repeatIntervalSec == 0) {
                                    Icon(
                                        imageVector = Icons.Rounded.AllInclusive,
                                        contentDescription = "Loop Continuous",
                                        tint = if (channel.isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = "Repeat Interval",
                                        tint = if (channel.isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${channel.repeatIntervalSec}s${if (channel.isFuzzyRepeat) " ~" else ""}",
                                        fontSize = 11.sp,
                                        color = if (channel.isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Slider(
                        value = channel.volume.toFloat(),
                        onValueChange = { onVolumeChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = !channel.isMuted,
                        colors = SliderDefaults.colors(
                            thumbColor = if (channel.isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            activeTrackColor = if (channel.isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                    )
                }

                IconButton(
                    onClick = onChannelDetailsClicked,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Channel Settings",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CH ${channel.channelId + 1} — Tap to invite a sound...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
        }
    }
}
