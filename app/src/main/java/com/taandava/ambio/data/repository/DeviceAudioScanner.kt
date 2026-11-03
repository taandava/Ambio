package com.taandava.ambio.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.taandava.ambio.data.model.SoundItem

object DeviceAudioScanner {
    fun scanDeviceAudio(context: Context): List<SoundItem> {
        val list = mutableListOf<SoundItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= 500"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Untitled Audio"
                    val artist = cursor.getString(artistCol) ?: ""
                    val duration = cursor.getLong(durCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    list.add(
                        SoundItem(
                            id = contentUri.toString(),
                            name = title,
                            uriString = contentUri.toString(),
                            artist = if (artist.equals("<unknown>", ignoreCase = true)) "" else artist,
                            durationMs = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AudioScan", "Error scanning device audio", e)
        }
        return list
    }
}
