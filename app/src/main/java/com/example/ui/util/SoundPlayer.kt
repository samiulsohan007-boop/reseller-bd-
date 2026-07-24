package com.example.ui.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator

object SoundPlayer {
    fun playNotificationSound(context: Context) {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
            if (ringtone != null) {
                ringtone.play()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback tone generator for messenger-like beep sound
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
