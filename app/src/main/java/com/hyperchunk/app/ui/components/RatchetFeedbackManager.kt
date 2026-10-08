package com.hyperchunk.app.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import com.hyperchunk.app.R
import kotlin.random.Random

/**
 * Handles tactile haptic pulses and realistic mechanical ratchet click sound effects
 * for wheel pickers and rotational dials.
 */
class RatchetFeedbackManager(context: Context) {

    companion object {
        private const val TAG = "RatchetFeedback"
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var soundId: Int = 0
    @Volatile
    private var isLoaded: Boolean = false

    init {
        try {
            soundId = soundPool.load(context.applicationContext, R.raw.ratchet_tick, 1)
            soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == soundId) {
                    isLoaded = true
                    Log.d(TAG, "Ratchet tick sample loaded successfully (soundId=$soundId)")
                } else {
                    Log.w(TAG, "Ratchet tick sample load failed: status=$status, sampleId=$sampleId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Ratchet SoundPool", e)
        }
    }

    /**
     * Plays a crisp mechanical ratchet tick and delivers a tactile clock tick haptic pulse.
     */
    fun playRatchet(view: View?, pitchVariance: Float? = null) {
        // Haptic feedback (watch clock tick sensation)
        view?.let { v ->
            val performed = v.performHapticFeedback(
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            if (!performed) {
                v.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            }
        }

        // Mechanical acoustic click
        if (isLoaded && soundId != 0) {
            val rate = pitchVariance ?: (0.97f + Random.nextFloat() * 0.06f)
            val streamId = soundPool.play(soundId, 1.0f, 1.0f, 1, 0, rate)
            Log.d(TAG, "Played ratchet click streamId=$streamId, rate=$rate")
        }
    }

    fun release() {
        try {
            soundPool.release()
            Log.d(TAG, "Released Ratchet SoundPool")
        } catch (_: Exception) {}
    }
}
