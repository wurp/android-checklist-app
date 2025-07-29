package com.checklist.app.presentation.utils

import android.content.Context
import android.media.RingtoneManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    open fun playCompletionChime() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notification)
            ringtone.play()
        } catch (e: Exception) {
            // Silently fail if unable to play sound
        }
    }
}