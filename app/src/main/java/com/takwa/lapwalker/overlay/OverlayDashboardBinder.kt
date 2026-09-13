package com.takwa.lapwalker.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.SeekBar
import com.takwa.lapwalker.databinding.OverlayDashboardBinding
import com.takwa.lapwalker.domain.model.WalkSessionState
import java.util.Locale

class OverlayDashboardBinder(
    context: Context,
    private val windowManager: WindowManager,
    private val onMinimize: () -> Unit,
    private val onStopWalk: () -> Unit,
    private val onOpacityChanged: (Float) -> Unit
) {
    val binding: OverlayDashboardBinding = OverlayDashboardBinding.inflate(LayoutInflater.from(context))
    val layoutParams: WindowManager.LayoutParams
    var isVisible = false
        private set

    init {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val minimizeListener = View.OnClickListener { onMinimize() }
        binding.btnDashMinimize.setOnClickListener(minimizeListener)
        binding.btnDashClose.setOnClickListener(minimizeListener)
        binding.btnDashStop.setOnClickListener { onStopWalk() }

        binding.seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val alpha = (30 + progress) / 100f
                    binding.tvOpacityValue.text = "${(alpha * 100).toInt()}%"
                    onOpacityChanged(alpha)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun syncOpacity(alpha: Float) {
        val progress = ((alpha * 100).toInt() - 30).coerceIn(0, 70)
        binding.seekOpacity.progress = progress
        binding.tvOpacityValue.text = "${(alpha * 100).toInt()}%"
    }

    fun expand(state: WalkSessionState, steps: Int = 0) {
        if (isVisible) return
        render(state)
        renderSteps(steps)

        try {
            binding.root.apply {
                scaleX = 0.75f
                scaleY = 0.75f
                alpha = 0f
            }
            windowManager.addView(binding.root, layoutParams)
            isVisible = true

            binding.root.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()
        } catch (_: Exception) {}
    }

    fun renderSteps(steps: Int) {
        binding.tvDashSteps.text = String.format(Locale.US, "%,d", steps)
    }

    fun collapse() {
        if (!isVisible) return

        binding.root.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(160)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                try {
                    windowManager.removeView(binding.root)
                } catch (_: Exception) {}
                isVisible = false
            }
            .start()
    }

    fun render(state: WalkSessionState) {
        binding.tvDashTime.text = state.formattedTime
        binding.tvDashLaps.text = state.laps.toString()

        binding.tvDashAvgTime.text = if (state.laps > 0 && state.elapsedSeconds > 0) {
            String.format(Locale.US, "%.1fs", state.avgLapTimeSec)
        } else {
            "--s"
        }

        binding.tvDashDistance.text = String.format(Locale.US, "%.2f km", state.currentDistanceKm)
        binding.tvDashCalories.text = String.format(Locale.US, "%.0f kcal", state.caloriesBurned)
        binding.tvDashTargetKm.text = String.format(Locale.US, "%.2f km", state.targetKm)

        if (state.isTargetCompleted) {
            binding.tvDashRemainingKm.text = "🎉 Target Goal Completed!"
            binding.tvDashRemainingKm.setTextColor(0xFF00E676.toInt())
            binding.tvDashTargetSub.text = String.format(
                Locale.US,
                "%.2f km reached in %d laps",
                state.currentDistanceKm,
                state.laps
            )
        } else {
            binding.tvDashRemainingKm.text = String.format(Locale.US, "%.2f km remaining", state.remainingKm)
            binding.tvDashRemainingKm.setTextColor(0xFFFFFFFF.toInt())
            binding.tvDashTargetSub.text = String.format(
                Locale.US,
                "%.2f / %.2f km • %d laps left",
                state.currentDistanceKm,
                state.targetKm,
                state.remainingLaps
            )
        }

        binding.progressTarget.progress = state.progressPercent
    }

    fun detach() {
        if (isVisible) {
            try {
                windowManager.removeView(binding.root)
            } catch (_: Exception) {}
            isVisible = false
        }
    }
}
