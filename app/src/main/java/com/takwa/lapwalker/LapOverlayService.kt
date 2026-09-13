package com.takwa.lapwalker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.takwa.lapwalker.domain.model.WalkSessionState
import com.takwa.lapwalker.domain.usecase.SaveWorkoutUseCase
import com.takwa.lapwalker.overlay.OverlayBubbleBinder
import com.takwa.lapwalker.overlay.OverlayDashboardBinder
import com.takwa.lapwalker.overlay.OverlayDismissTargetBinder
import com.takwa.lapwalker.overlay.OverlayIntent
import com.takwa.lapwalker.overlay.WalkSessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Locale

class LapOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "lap_walker_overlay_channel"
        private const val NOTIFICATION_ID = 101
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val saveWorkoutUseCase: SaveWorkoutUseCase by inject()
    private val stepSensorManager: com.takwa.lapwalker.core.sensors.StepSensorManager by inject()
    private lateinit var walkEngine: WalkSessionEngine

    private lateinit var windowManager: WindowManager
    private var vibrator: Vibrator? = null

    private lateinit var bubbleBinder: OverlayBubbleBinder
    private lateinit var dashboardBinder: OverlayDashboardBinder
    private lateinit var dismissTargetBinder: OverlayDismissTargetBinder

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        walkEngine = WalkSessionEngine(serviceScope)
        stepSensorManager.setSessionActive(true)
        stepSensorManager.startListening()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("LapWalker ready - Timer running at 0 laps"))

        initBinders()
        observeSessionState()
    }

    private fun initBinders() {
        dismissTargetBinder = OverlayDismissTargetBinder(
            context = this,
            windowManager = windowManager
        )

        bubbleBinder = OverlayBubbleBinder(
            context = this,
            windowManager = windowManager,
            onSingleTap = { onBubbleSingleTapped() },
            onDoubleTap = { toggleDashboard() },
            onDragStarted = { dismissTargetBinder.show() },
            onDragMoved = { cx, cy -> dismissTargetBinder.checkProximity(cx, cy) },
            onDragEnded = { isDismissed ->
                dismissTargetBinder.hide()
                if (isDismissed) {
                    stopSessionAndService()
                }
            }
        )

        dashboardBinder = OverlayDashboardBinder(
            context = this,
            windowManager = windowManager,
            onMinimize = { dashboardBinder.collapse() },
            onStopWalk = { stopSessionAndService() },
            onOpacityChanged = { alpha ->
                bubbleBinder.setOpacity(alpha)
            }
        )

        bubbleBinder.attach()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val feet = intent.getFloatExtra(MainActivity.KEY_LAP_FEET, 36.0f)
            val target = intent.getFloatExtra(MainActivity.KEY_TARGET_KM, 1.0f)
            val weight = intent.getFloatExtra(MainActivity.KEY_WEIGHT, 70.0f)
            val vibrate = intent.getBooleanExtra(MainActivity.KEY_VIBRATE, true)

            walkEngine.dispatch(
                OverlayIntent.Initialize(
                    lapLengthFeet = feet,
                    targetKm = target,
                    weightKg = weight,
                    vibrateEnabled = vibrate
                )
            )
        }
        return START_STICKY
    }

    private fun onBubbleSingleTapped() {
        triggerHaptic()

        val nextLap = walkEngine.sessionState.value.laps + 1
        walkEngine.dispatch(OverlayIntent.RecordLap)

        bubbleBinder.animateLapCount(nextLap.toString(), if (nextLap == 1) "LAP" else "LAPS")
    }

    private fun toggleDashboard() {
        if (dashboardBinder.isVisible) {
            dashboardBinder.collapse()
        } else {
            dashboardBinder.expand(walkEngine.sessionState.value, stepSensorManager.todaySteps.value)
        }
    }

    private fun triggerHaptic() {
        if (!walkEngine.sessionState.value.vibrateEnabled) return
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    private fun observeSessionState() {
        serviceScope.launch {
            walkEngine.sessionState.collect { state ->
                bubbleBinder.render(state)
                if (dashboardBinder.isVisible) {
                    dashboardBinder.render(state)
                    dashboardBinder.renderSteps(stepSensorManager.todaySteps.value)
                }
                updateNotification(state)
            }
        }

        serviceScope.launch {
            stepSensorManager.todaySteps.collect { steps ->
                if (dashboardBinder.isVisible) {
                    dashboardBinder.renderSteps(steps)
                }
            }
        }
    }

    private fun stopSessionAndService() {
        val finalState = walkEngine.sessionState.value
        serviceScope.launch {
            try {
                saveWorkoutUseCase(finalState)
            } catch (_: Exception) {}
            stepSensorManager.setSessionActive(false)
            walkEngine.dispatch(OverlayIntent.StopSession)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LapWalker Active Walking Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows current laps, distance, and walk time"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LapWalker - Walking in Garage")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(state: WalkSessionState) {
        val text = String.format(
            Locale.US,
            "Dist: %.2f km | Laps: %d | Time: %s",
            state.currentDistanceKm,
            state.laps,
            state.formattedTime
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        walkEngine.stop()
        stepSensorManager.setSessionActive(false)
        stepSensorManager.stopListening()
        dismissTargetBinder.detach()
        bubbleBinder.detach()
        dashboardBinder.detach()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
