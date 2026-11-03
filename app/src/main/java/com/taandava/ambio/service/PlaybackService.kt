package com.taandava.ambio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.taandava.ambio.MainActivity
import com.taandava.ambio.R
import com.taandava.ambio.data.model.MixChannel
import com.taandava.ambio.data.model.SavedMix
import com.taandava.ambio.data.model.SoundItem
import com.taandava.ambio.data.repository.MixStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt
import kotlin.random.Random

data class MixPlaybackState(
    val isPlaying: Boolean = false,
    val activeChannelsCount: Int = 0,
    val masterVolume: Int = 100,
    val mixName: String = "My Mix",
    val sleepTimerRemainingSeconds: Long? = null,
)

class PlaybackService : MediaSessionService() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ambio_mix_channel"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_PLAY = "com.taandava.ambio.ACTION_PLAY"
        const val ACTION_STOP = "com.taandava.ambio.ACTION_STOP"
        const val ACTION_TOGGLE = "com.taandava.ambio.ACTION_TOGGLE"

        private val _state = MutableStateFlow(MixPlaybackState())
        val state: StateFlow<MixPlaybackState> = _state.asStateFlow()

        var instance: PlaybackService? = null
            private set
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    private val binder = LocalBinder()
    private var mediaSession: MediaSession? = null
    private lateinit var dummyMasterPlayer: ExoPlayer

    private val channelPlayers = mutableMapOf<Int, ExoPlayer>()
    private val channelListeners = mutableMapOf<Int, Player.Listener>()
    private val channelSchedulers = mutableMapOf<Int, Runnable>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeChannels: List<MixChannel> = emptyList()
    private var isPlaying = false
    private var masterVolume = 100
    private lateinit var mixStore: MixStore

    private var sleepTimerEndTimestamp: Long? = null
    private val sleepTimerRunnable = object : Runnable {
        override fun run() {
            val endMs = sleepTimerEndTimestamp
            if (endMs == null) {
                _state.update { it.copy(sleepTimerRemainingSeconds = null) }
                return
            }

            val now = System.currentTimeMillis()
            val remainingMs = endMs - now
            val remainingSec = (remainingMs / 1000L).coerceAtLeast(0L)

            if (remainingMs <= 0L) {
                cancelSleepTimer()
                stopPlayback()
            } else {
                if (remainingSec <= 15L) {
                    val fadeFactor = (remainingMs.toFloat() / 15000f).coerceIn(0f, 1f)
                    val currentFadedVolume = (masterVolume * fadeFactor).roundToInt()
                    applyMasterVolumeToPlayers(currentFadedVolume)
                }
                _state.update { it.copy(sleepTimerRemainingSeconds = remainingSec) }
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        mixStore = MixStore(this)
        masterVolume = mixStore.getMasterVolume()

        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        dummyMasterPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, false)
            .build()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, dummyMasterPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        val activeMix = mixStore.getActiveMix()
        activeChannels = activeMix?.channels ?: emptyList()
        _state.update {
            it.copy(
                isPlaying = false,
                activeChannelsCount = activeChannels.count { ch -> (ch.sound != null) && (!ch.isMuted) },
                masterVolume = masterVolume,
                mixName = activeMix?.name ?: "No Mix",
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> startPlayback()
            ACTION_STOP -> stopPlayback()
            ACTION_TOGGLE -> togglePlayback()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        val durationMs = minutes * 60 * 1000L
        sleepTimerEndTimestamp = System.currentTimeMillis() + durationMs
        mainHandler.removeCallbacks(sleepTimerRunnable)
        _state.update { it.copy(sleepTimerRemainingSeconds = (durationMs / 1000L)) }
        mainHandler.post(sleepTimerRunnable)
    }

    fun cancelSleepTimer() {
        sleepTimerEndTimestamp = null
        mainHandler.removeCallbacks(sleepTimerRunnable)
        applyMasterVolumeToPlayers(masterVolume)
        _state.update { it.copy(sleepTimerRemainingSeconds = null) }
    }

    fun setMasterVolume(vol: Int) {
        masterVolume = vol.coerceIn(0, 100)
        mixStore.setMasterVolume(masterVolume)
        applyMasterVolumeToPlayers(masterVolume)
        _state.update { it.copy(masterVolume = masterVolume) }
    }

    private fun applyMasterVolumeToPlayers(mVol: Int) {
        val masterFactor = mVol / 100f
        for ((channelId, _, _, volume, isMuted, _, _, _, _) in activeChannels) {
            val player = channelPlayers[channelId] ?: continue
            val eff = if (isMuted) 0f else (volume / 100f) * masterFactor
            player.volume = eff
        }
    }

    fun setMix(mix: SavedMix, autoPlay: Boolean = isPlaying) {
        activeChannels = mix.channels
        mixStore.saveMix(mix)

        val currentIds = mix.channels.asSequence().map { it.channelId }.toSet()
        val toRemove = channelPlayers.keys.filter { it !in currentIds }
        for (id in toRemove) {
            releaseChannelPlayer(id)
        }

        for (ch in mix.channels) {
            val existing = channelPlayers[ch.channelId]
            if (ch.sound == null) {
                releaseChannelPlayer(ch.channelId)
            } else {
                if (existing == null) {
                    initChannelPlayer(ch)
                } else {
                    existing.volume = if (ch.isMuted) 0f else (ch.volume / 100f) * (masterVolume / 100f)
                    existing.playbackParameters = PlaybackParameters(ch.speed, ch.pitch)
                    configurePlayerInterval(ch, existing)
                }
            }
        }

        val activeCount = mix.channels.count { ch -> (ch.sound != null) && (!ch.isMuted) }
        _state.update {
            it.copy(
                activeChannelsCount = activeCount,
                mixName = mix.name,
            )
        }

        if (isPlaying || autoPlay) {
            if (autoPlay) {
                startPlayback()
            } else {
                for ((chId, player) in channelPlayers) {
                    val ch = mix.channels.find { it.channelId == chId } ?: continue
                    if ((ch.repeatIntervalSec == 0) && (!player.isPlaying) && (!ch.isMuted)) {
                        player.play()
                    }
                }
                updateNotification()
            }
        }
    }

    fun toggleChannelMute(channelId: Int): Boolean {
        val ch = activeChannels.find { it.channelId == channelId } ?: return false
        val newMuted = !ch.isMuted
        val updatedCh = ch.copy(isMuted = newMuted)
        activeChannels = activeChannels.map { if (it.channelId == channelId) updatedCh else it }

        mixStore.getActiveMix()?.let {
            mixStore.saveMix(it.copy(channels = activeChannels))
        }

        val player = channelPlayers[channelId]
        if (player != null) {
            player.volume = if (newMuted) 0f else (updatedCh.volume / 100f) * (masterVolume / 100f)
            if (isPlaying && !newMuted && !player.isPlaying) {
                player.play()
            }
        }

        val activeCount = activeChannels.count { c -> (c.sound != null) && (!c.isMuted) }
        _state.update { it.copy(activeChannelsCount = activeCount) }
        updateNotification()
        return newMuted
    }

    fun updateChannelVolume(channelId: Int, volume: Int) {
        val ch = activeChannels.find { it.channelId == channelId } ?: return
        val updatedCh = ch.copy(volume = volume)
        activeChannels = activeChannels.map { if (it.channelId == channelId) updatedCh else it }

        mixStore.getActiveMix()?.let {
            mixStore.saveMix(it.copy(channels = activeChannels))
        }

        channelPlayers[channelId]?.let {
            it.volume = if (updatedCh.isMuted) 0f else (volume / 100f) * (masterVolume / 100f)
        }
    }

    fun updateChannelDetails(
        channelId: Int,
        customName: String?,
        repeatSec: Int,
        isFuzzy: Boolean,
        volume: Int,
        pitch: Float,
        speed: Float,
    ) {
        val ch = activeChannels.find { it.channelId == channelId } ?: return
        val updatedCh = ch.copy(
            customName = customName,
            repeatIntervalSec = repeatSec,
            isFuzzyRepeat = isFuzzy,
            volume = volume,
            pitch = pitch,
            speed = speed,
        )
        activeChannels = activeChannels.map { if (it.channelId == channelId) updatedCh else it }

        mixStore.getActiveMix()?.let {
            mixStore.saveMix(it.copy(channels = activeChannels))
        }

        val player = channelPlayers[channelId]
        if (player != null) {
            player.volume = if (updatedCh.isMuted) 0f else (updatedCh.volume / 100f) * (masterVolume / 100f)
            player.playbackParameters = PlaybackParameters(updatedCh.speed, updatedCh.pitch)
            configurePlayerInterval(updatedCh, player)
            if (isPlaying && (updatedCh.repeatIntervalSec == 0) && (!player.isPlaying) && (!updatedCh.isMuted)) {
                player.play()
            }
        }
    }

    fun removeChannel(channelId: Int) {
        val updated = activeChannels.map {
            if (it.channelId == channelId) MixChannel(channelId = channelId) else it
        }
        mixStore.getActiveMix()?.let {
            setMix(it.copy(channels = updated))
        }
    }

    fun setChannelSound(channelId: Int, sound: SoundItem?) {
        val current = activeChannels.toMutableList()
        val index = current.indexOfFirst { it.channelId == channelId }
        val newCh = if (index >= 0) {
            current[index].copy(sound = sound, customName = null, isMuted = false)
        } else {
            MixChannel(channelId = channelId, sound = sound)
        }
        if (index >= 0) current[index] = newCh else current.add(newCh)

        releaseChannelPlayer(channelId)
        mixStore.getActiveMix()?.let {
            setMix(it.copy(channels = current))
        }

        if ((isPlaying) && (sound != null)) {
            val player = channelPlayers[channelId]
            if ((newCh.repeatIntervalSec == 0) && (!newCh.isMuted)) {
                player?.play()
            }
        }
    }

    fun startPlayback() {
        if (activeChannels.none { ch -> (ch.sound != null) && (!ch.isMuted) }) return

        for (ch in activeChannels) {
            if ((ch.sound != null) && (!channelPlayers.containsKey(ch.channelId))) {
                initChannelPlayer(ch)
            }
        }

        isPlaying = true
        _state.update { it.copy(isPlaying = true) }

        for ((channelId, _, _, volume, isMuted, repeatIntervalSec, _, speed, pitch) in activeChannels) {
            val player = channelPlayers[channelId] ?: continue
            player.volume = if (isMuted) 0f else (volume / 100f) * (masterVolume / 100f)
            player.playbackParameters = PlaybackParameters(speed, pitch)
            if (repeatIntervalSec == 0) {
                player.play()
            } else {
                player.seekTo(0)
                player.play()
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification())
    }

    fun stopPlayback() {
        isPlaying = false
        _state.update { it.copy(isPlaying = false) }

        for (runnable in channelSchedulers.values) {
            mainHandler.removeCallbacks(runnable)
        }
        channelSchedulers.clear()

        for (player in channelPlayers.values) {
            player.pause()
            player.seekTo(0)
        }

        stopForeground(STOP_FOREGROUND_DETACH)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    fun togglePlayback() {
        if (isPlaying) stopPlayback() else startPlayback()
    }

    private fun initChannelPlayer(ch: MixChannel) {
        if (ch.sound == null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val p = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, false)
            .build()

        p.volume = if (ch.isMuted) 0f else (ch.volume / 100f) * (masterVolume / 100f)
        p.playbackParameters = PlaybackParameters(ch.speed, ch.pitch)

        configurePlayerInterval(ch, p)
        channelPlayers[ch.channelId] = p
    }

    private fun configurePlayerInterval(ch: MixChannel, p: ExoPlayer) {
        val sound = ch.sound ?: return
        p.clearMediaItems()
        val mediaItem = MediaItem.fromUri(sound.uriString.toUri())
        p.setMediaItem(mediaItem)
        p.prepare()

        channelListeners.remove(ch.channelId)?.let { p.removeListener(it) }
        channelSchedulers.remove(ch.channelId)?.let { mainHandler.removeCallbacks(it) }

        if (ch.repeatIntervalSec == 0) {
            p.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            p.repeatMode = Player.REPEAT_MODE_OFF
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if ((playbackState == Player.STATE_ENDED) && isPlaying && (!ch.isMuted)) {
                        scheduleNextIntermittentPlay(ch)
                    }
                }
            }
            channelListeners[ch.channelId] = listener
            p.addListener(listener)
        }
    }

    private fun scheduleNextIntermittentPlay(ch: MixChannel) {
        val intervalSec = if (ch.isFuzzyRepeat) {
            val variance = 0.7 + (Random.nextDouble() * 0.6)
            (ch.repeatIntervalSec * variance).roundToInt().coerceAtLeast(1)
        } else {
            ch.repeatIntervalSec
        }

        val runnable = Runnable {
            if (isPlaying && !ch.isMuted) {
                val player = channelPlayers[ch.channelId]
                player?.seekTo(0)
                player?.play()
            }
        }
        channelSchedulers[ch.channelId] = runnable
        mainHandler.postDelayed(runnable, intervalSec * 1000L)
    }

    private fun releaseChannelPlayer(channelId: Int) {
        channelListeners.remove(channelId)?.let { listener ->
            channelPlayers[channelId]?.removeListener(listener)
        }
        channelSchedulers.remove(channelId)?.let { mainHandler.removeCallbacks(it) }
        channelPlayers.remove(channelId)?.let {
            it.stop()
            it.release()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activeCount = activeChannels.count { ch -> (ch.sound != null) && (!ch.isMuted) }
        val title = if (isPlaying) "Ambio — Playing" else "Ambio — Standby"
        val subtitle = "$activeCount active sound tracks"

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Stop" else "Play",
                toggleIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Clear", stopIntent)

        mediaSession?.let {
            builder.setStyle(androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(it))
        }

        return builder.build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        instance = null
        cancelSleepTimer()

        for (runnable in channelSchedulers.values) {
            mainHandler.removeCallbacks(runnable)
        }
        channelSchedulers.clear()

        for ((id, player) in channelPlayers) {
            channelListeners[id]?.let { player.removeListener(it) }
            player.stop()
            player.release()
        }
        channelListeners.clear()
        channelPlayers.clear()
        dummyMasterPlayer.release()
        mediaSession?.release()
        super.onDestroy()
    }
}
