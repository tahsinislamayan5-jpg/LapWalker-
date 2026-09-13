package com.takwa.lapwalker.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import com.takwa.lapwalker.R
import com.takwa.lapwalker.databinding.OverlayDismissTargetBinding
import kotlin.math.hypot

class OverlayDismissTargetBinder(
    private val context: Context,
    private val windowManager: WindowManager
) {
    val binding: OverlayDismissTargetBinding = OverlayDismissTargetBinding.inflate(LayoutInflater.from(context))
    val layoutParams: WindowManager.LayoutParams

    private var isAttached = false
    var isInDismissZone = false
        private set

    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val magnetRadius: Float
    private val targetYOffset: Int

    init {
        val density = context.resources.displayMetrics.density
        magnetRadius = 85 * density
        targetYOffset = (55 * density).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val windowSize = (120 * density).toInt()
        layoutParams = WindowManager.LayoutParams(
            windowSize,
            windowSize,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = targetYOffset
        }
    }

    fun show() {
        if (isAttached) return
        try {
            binding.root.apply {
                alpha = 0f
                scaleX = 0.5f
                scaleY = 0.5f
            }
            windowManager.addView(binding.root, layoutParams)
            isAttached = true

            binding.root.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        } catch (_: Exception) {}
    }

    fun hide() {
        if (!isAttached) return
        isInDismissZone = false
        binding.targetCircle.setBackgroundResource(R.drawable.dismiss_target_bg)

        binding.root.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(160)
            .withEndAction {
                try {
                    windowManager.removeView(binding.root)
                } catch (_: Exception) {}
                isAttached = false
            }
            .start()
    }

    /**
     * Checks if the bubble is dragged close to the dismiss target.
     * Triggers magnet scale expansion & haptic vibration when entering the zone.
     */
    fun checkProximity(bubbleCenterX: Float, bubbleCenterY: Float): Boolean {
        if (!isAttached) return false

        val location = IntArray(2)
        binding.targetCircle.getLocationOnScreen(location)
        val targetCenterX = location[0] + (binding.targetCircle.width / 2f)
        val targetCenterY = location[1] + (binding.targetCircle.height / 2f)

        val distance = hypot((bubbleCenterX - targetCenterX).toDouble(), (bubbleCenterY - targetCenterY).toDouble()).toFloat()

        if (distance <= magnetRadius) {
            if (!isInDismissZone) {
                isInDismissZone = true
                triggerHapticMagnet()
                binding.targetCircle.setBackgroundResource(R.drawable.dismiss_target_active_bg)
                binding.targetCircle.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(120)
                    .start()
            }
            return true
        } else {
            if (isInDismissZone) {
                isInDismissZone = false
                binding.targetCircle.setBackgroundResource(R.drawable.dismiss_target_bg)
                binding.targetCircle.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            return false
        }
    }

    private fun triggerHapticMagnet() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(55)
            }
        } catch (_: Exception) {}
    }

    fun detach() {
        if (isAttached) {
            try {
                windowManager.removeView(binding.root)
            } catch (_: Exception) {}
            isAttached = false
        }
    }
}
