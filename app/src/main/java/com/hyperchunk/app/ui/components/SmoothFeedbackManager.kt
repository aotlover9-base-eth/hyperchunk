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
 * Handles subtle, tactile haptic clock pulses and smooth acoustic micro-ticks
 * for the wheel time picker (resembling luxury mechanical watch crowns and Apple Watch dials).
 */
class SmoothFeedbackManager(context: Context) {

    companion object {
        private const val TAG = "SmoothFeedback"
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
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
            soundId = soundPool.load(context.applicationContext, R.raw.smooth_dial_tick, 1)
            soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == soundId) {
                    isLoaded = true
                    Log.d(TAG, "Smooth dial tick sample loaded successfully (soundId=$soundId)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SmoothFeedback SoundPool", e)
        }
    }

    /**
     * Triggers a smooth haptic pulse and a faint, silky acoustic micro-tick.
     */
    fun playTick(view: View?, pitchVariance: Float? = null) {
        // Velvety Clock Tick Haptic Pulse
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

        // Soft, subtle micro-click (0.35f volume ensures it is non-intrusive)
        if (isLoaded && soundId != 0) {
            val rate = pitchVariance ?: (0.98f + Random.nextFloat() * 0.04f)
            soundPool.play(soundId, 0.35f, 0.35f, 1, 0, rate)
        }
    }

    fun release() {
        try {
            soundPool.release()
        } catch (_: Exception) {}
    }
}
