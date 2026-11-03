package com.taandava.ambio.data.model

import java.util.UUID

data class SoundItem(
    val id: String,
    val name: String,
    val uriString: String,
    val artist: String = "",
    val durationMs: Long = 0L,
)

data class MixChannel(
    val channelId: Int,
    val sound: SoundItem? = null,
    val customName: String? = null,
    val volume: Int = 50,
    val isMuted: Boolean = false,
    val repeatIntervalSec: Int = 0,
    val isFuzzyRepeat: Boolean = false,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
) {
    val displayName: String
        get() = customName?.takeIf { it.isNotBlank() } ?: sound?.name ?: "Channel ${channelId + 1}"
}

data class SavedMix(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val channels: List<MixChannel> = emptyList(),
)
