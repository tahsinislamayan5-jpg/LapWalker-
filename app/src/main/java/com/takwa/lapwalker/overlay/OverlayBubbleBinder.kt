package com.takwa.lapwalker.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import com.takwa.lapwalker.databinding.OverlayBubbleBinding
import com.takwa.lapwalker.domain.model.WalkSessionState
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class OverlayBubbleBinder(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onDragStarted: () -> Unit,
    private val onDragMoved: (bubbleCenterX: Float, bubbleCenterY: Float) -> Boolean,
    private val onDragEnded: (isDismissed: Boolean) -> Unit
) {
    val binding: OverlayBubbleBinding = OverlayBubbleBinding.inflate(LayoutInflater.from(context))
    val layoutParams: WindowManager.LayoutParams

    private val screenWidth: Int
    private val screenHeight: Int
    private val bubbleSize: Int
    private val windowPadding: Int
    private val totalWindowSize: Int
    private val edgePadding: Int
    private val statusBarHeight: Int
    private val navBarHeight: Int

    private var snapAnimator: ValueAnimator? = null
    private val gestureDetector: GestureDetector
    private var velocityTracker: VelocityTracker? = null

    private var currentOpacity: Float = 1.0f
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        val metrics = context.resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val density = metrics.density

        bubbleSize = (56 * density).toInt()
        windowPadding = (10 * density).toInt()
        totalWindowSize = (76 * density).toInt()
        edgePadding = (8 * density).toInt()
        statusBarHeight = (28 * density).toInt()
        navBarHeight = (52 * density).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            totalWindowSize,
            totalWindowSize,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - totalWindowSize - edgePadding + windowPadding
            y = screenHeight / 3
        }

        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTap()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                cycleGhostOpacity()
            }
        })

        setupTouchHandling(touchSlop)
    }

    private fun setupTouchHandling(touchSlop: Int) {
        binding.root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(event)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        snapAnimator?.cancel()
                        velocityTracker?.recycle()
                        velocityTracker = VelocityTracker.obtain().apply {
                            addMovement(event)
                        }

                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false

                        binding.bubbleInner.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        velocityTracker?.addMovement(event)

                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY

                        if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                            isDragging = true
                            onDragStarted()
                        }

                        if (isDragging) {
                            val newX = initialX + dx.toInt()
                            val newY = initialY + dy.toInt()

                            val minX = -windowPadding
                            val maxX = screenWidth - totalWindowSize + windowPadding
                            val minY = statusBarHeight - windowPadding
                            val maxY = screenHeight - navBarHeight - totalWindowSize + windowPadding

                            layoutParams.x = max(minX, min(newX, maxX))
                            layoutParams.y = max(minY, min(newY, maxY))

                            try {
                                windowManager.updateViewLayout(binding.root, layoutParams)
                            } catch (_: Exception) {}

                            val bubbleCenterX = layoutParams.x + (totalWindowSize / 2f)
                            val bubbleCenterY = layoutParams.y + (totalWindowSize / 2f)
                            onDragMoved(bubbleCenterX, bubbleCenterY)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        binding.bubbleInner.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()

                        if (isDragging) {
                            velocityTracker?.addMovement(event)
                            velocityTracker?.computeCurrentVelocity(1000)
                            val xVelocity = velocityTracker?.xVelocity ?: 0f

                            val bubbleCenterX = layoutParams.x + (totalWindowSize / 2f)
                            val bubbleCenterY = layoutParams.y + (totalWindowSize / 2f)
                            val isDismissed = onDragMoved(bubbleCenterX, bubbleCenterY)

                            onDragEnded(isDismissed)

                            if (!isDismissed) {
                                snapWithFlingVelocity(xVelocity)
                            }
                        }

                        velocityTracker?.recycle()
                        velocityTracker = null
                        return true
                    }
                }
                return false
            }
        })
    }

    /**
     * True fling/flick momentum physics:
     * If user flicks with sufficient speed (|xVelocity| > 600 dp/s),
     * the bubble flies toward that edge regardless of current position!
     */
    private fun snapWithFlingVelocity(xVelocity: Float) {
        val flingThreshold = 600f

        val targetX = when {
            xVelocity < -flingThreshold -> edgePadding - windowPadding // Flicked Left!
            xVelocity > flingThreshold -> screenWidth - totalWindowSize - edgePadding + windowPadding // Flicked Right!
            else -> {
                // Not flicked hard: snap to closest edge
                val bubbleCenterX = layoutParams.x + (totalWindowSize / 2)
                if (bubbleCenterX < screenWidth / 2) {
                    edgePadding - windowPadding
                } else {
                    screenWidth - totalWindowSize - edgePadding + windowPadding
                }
            }
        }

        val minY = statusBarHeight + edgePadding - windowPadding
        val maxY = screenHeight - navBarHeight - totalWindowSize - edgePadding + windowPadding
        val targetY = max(minY, min(layoutParams.y, maxY))
        val startY = layoutParams.y
        val startX = layoutParams.x

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            val distance = hypot((targetX - startX).toDouble(), (targetY - startY).toDouble()).toFloat()
            duration = max(180L, min(360L, (distance * 0.7f).toLong()))
            interpolator = OvershootInterpolator(1.35f)
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                layoutParams.x = (startX + (targetX - startX) * fraction).toInt()
                layoutParams.y = (startY + (targetY - startY) * fraction).toInt()
                try {
                    windowManager.updateViewLayout(binding.root, layoutParams)
                } catch (_: Exception) {}
            }
            start()
        }
    }

    private fun cycleGhostOpacity() {
        val nextOpacity = when {
            currentOpacity > 0.85f -> 0.70f
            currentOpacity > 0.55f -> 0.40f
            else -> 1.0f
        }
        setOpacity(nextOpacity)
        triggerHaptic(40)
        val percent = (nextOpacity * 100).toInt()
        Toast.makeText(context, "👻 Ghost Mode: $percent% opacity", Toast.LENGTH_SHORT).show()
    }

    fun setOpacity(alpha: Float) {
        currentOpacity = alpha
        binding.bubbleInner.alpha = alpha
    }

    private fun triggerHaptic(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun attach() {
        binding.bubbleInner.apply {
            scaleX = 0.2f
            scaleY = 0.2f
            alpha = 0f
        }
        windowManager.addView(binding.root, layoutParams)

        binding.bubbleInner.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(currentOpacity)
            .setDuration(350)
            .setInterpolator(OvershootInterpolator(1.8f))
            .start()
    }

    fun render(state: WalkSessionState) {
        binding.tvBubbleCount.text = state.laps.toString()
        binding.tvBubbleLabel.text = if (state.laps == 1) "LAP" else "LAPS"
    }

    fun animateLapCount(newCount: String, newLabel: String) {
        binding.bubbleInner.animate().cancel()
        binding.bubbleInner.apply {
            scaleX = 1.0f
            scaleY = 1.0f
        }
        binding.bubbleInner.animate()
            .scaleX(1.22f)
            .scaleY(1.22f)
            .setDuration(90)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.bubbleInner.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(2.4f))
                    .start()
            }
            .start()

        binding.tvBubbleCount.animate().cancel()
        binding.tvBubbleCount.animate()
            .alpha(0f)
            .scaleY(0.6f)
            .setDuration(60)
            .withEndAction {
                binding.tvBubbleCount.text = newCount
                binding.tvBubbleLabel.text = newLabel
                binding.tvBubbleCount.scaleY = 1.3f
                binding.tvBubbleCount.animate()
                    .alpha(1.0f)
                    .scaleY(1.0f)
                    .setDuration(160)
                    .setInterpolator(OvershootInterpolator(1.8f))
                    .start()
            }
            .start()
    }

    fun detach() {
        snapAnimator?.cancel()
        velocityTracker?.recycle()
        velocityTracker = null
        try {
            windowManager.removeView(binding.root)
        } catch (_: Exception) {}
    }
}
