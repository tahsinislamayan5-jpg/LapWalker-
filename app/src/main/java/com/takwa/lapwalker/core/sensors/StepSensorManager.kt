package com.takwa.lapwalker.core.sensors

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepSensorManager(
    private val context: Context
) : SensorEventListener {

    companion object {
        private const val PREFS_NAME = "lap_walker_step_prefs"
        private const val KEY_SAVED_DATE = "saved_date"
        private const val KEY_LAST_RAW_STEPS = "last_raw_steps"
        private const val KEY_ACCUMULATED_STEPS = "accumulated_steps"
        private const val KEY_STEP_GOAL = "step_goal"
        private const val KEY_SESSION_ONLY_MODE = "session_only_mode"
        const val DEFAULT_STEP_GOAL = 10000

        // Google / Samsung Biomechanical Cadence Constants
        private const val MIN_STEP_INTERVAL_MS = 280L // ~3.5 Hz max human cadence limit
        private const val MAX_STEP_CADENCE_MS = 2500L // If pause > 2.5s, walking streak breaks
        private const val MIN_CONFIRMATION_STEPS = 8 // Must take >= 8 continuous steps to confirm walking
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    val isSensorAvailable: Boolean = stepCounterSensor != null || stepDetectorSensor != null

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _stepGoal = MutableStateFlow(prefs.getInt(KEY_STEP_GOAL, DEFAULT_STEP_GOAL))
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _sessionOnlyMode = MutableStateFlow(prefs.getBoolean(KEY_SESSION_ONLY_MODE, false))
    val sessionOnlyMode: StateFlow<Boolean> = _sessionOnlyMode.asStateFlow()

    private var isListening = false
    private var isSessionActive = false

    // Debounce & Cadence State Variables
    private var lastStepTimeMs: Long = 0L
    private var candidateStreak: Int = 0
    private var isStreakConfirmed: Boolean = false

    init {
        loadPersistedSteps()
    }

    fun setSessionOnlyMode(enabled: Boolean) {
        _sessionOnlyMode.value = enabled
        prefs.edit().putBoolean(KEY_SESSION_ONLY_MODE, enabled).apply()
    }

    fun setSessionActive(active: Boolean) {
        isSessionActive = active
        if (!active) {
            candidateStreak = 0
            isStreakConfirmed = false
        }
    }

    fun startListening() {
        if (isListening || sensorManager == null) return

        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        } else if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun loadPersistedSteps() {
        checkAndHandleDateChange()
        _todaySteps.value = prefs.getInt(KEY_ACCUMULATED_STEPS, 0)
    }

    private fun checkAndHandleDateChange(): Boolean {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_SAVED_DATE, null)

        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_SAVED_DATE, today)
                .putInt(KEY_ACCUMULATED_STEPS, 0)
                .putFloat(KEY_LAST_RAW_STEPS, -1f)
                .apply()
            _todaySteps.value = 0
            candidateStreak = 0
            isStreakConfirmed = false
            return true
        }
        return false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        checkAndHandleDateChange()

        // 1. Session-Only Gating: If session-only mode is active and no session is running, ignore
        if (_sessionOnlyMode.value && !isSessionActive) {
            if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                // Keep raw counter baseline fresh so next session starts clean
                prefs.edit().putFloat(KEY_LAST_RAW_STEPS, event.values[0]).apply()
            }
            return
        }

        val nowMs = SystemClock.elapsedRealtime()

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val currentRaw = event.values[0]
            val lastRaw = prefs.getFloat(KEY_LAST_RAW_STEPS, -1f)

            if (lastRaw < 0f) {
                prefs.edit().putFloat(KEY_LAST_RAW_STEPS, currentRaw).apply()
                lastStepTimeMs = nowMs
                return
            }

            // Reboot detection: raw step counter resets to 0 upon phone reboot
            if (currentRaw < lastRaw) {
                prefs.edit().putFloat(KEY_LAST_RAW_STEPS, currentRaw).apply()
                lastStepTimeMs = nowMs
                return
            }

            val rawDelta = (currentRaw - lastRaw).toInt()
            if (rawDelta <= 0) return

            // Always update lastRaw reference
            prefs.edit().putFloat(KEY_LAST_RAW_STEPS, currentRaw).apply()

            val timeDeltaMs = if (lastStepTimeMs > 0L) (nowMs - lastStepTimeMs).coerceAtLeast(1L) else 1000L
            lastStepTimeMs = nowMs

            // Speed & Vibration Sanity Filter:
            // Maximum human physical speed: ~3.5 steps/second (minimum 280ms per step)
            val maxPossibleHumanSteps = (timeDeltaMs / MIN_STEP_INTERVAL_MS).toInt().coerceAtLeast(1)

            // If rawDelta significantly exceeds max possible human steps for this time window,
            // the phone is in a car, on a vibration table, or shaking violently. Filter it!
            val validatedSteps = if (rawDelta > maxPossibleHumanSteps * 2) {
                0
            } else {
                rawDelta.coerceAtMost(maxPossibleHumanSteps)
            }

            if (validatedSteps > 0) {
                processCandidateSteps(validatedSteps, timeDeltaMs)
            }

        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            val timeDeltaMs = if (lastStepTimeMs > 0L) (nowMs - lastStepTimeMs) else 1000L

            // Drop events arriving faster than 280ms (vibration / car filter)
            if (timeDeltaMs < MIN_STEP_INTERVAL_MS) {
                return
            }

            lastStepTimeMs = nowMs
            processCandidateSteps(1, timeDeltaMs)
        }
    }

    private fun processCandidateSteps(stepCount: Int, timeDeltaMs: Long) {
        // If too much time has passed since the last step (> 2.5s), walking cadence streak is broken
        if (timeDeltaMs > MAX_STEP_CADENCE_MS) {
            candidateStreak = 0
            isStreakConfirmed = false
        }

        if (isStreakConfirmed) {
            addSteps(stepCount)
        } else {
            candidateStreak += stepCount
            if (candidateStreak >= MIN_CONFIRMATION_STEPS) {
                isStreakConfirmed = true
                addSteps(candidateStreak)
                candidateStreak = 0
            }
        }
    }

    private fun addSteps(count: Int) {
        val current = prefs.getInt(KEY_ACCUMULATED_STEPS, 0)
        val newTotal = current + count
        prefs.edit().putInt(KEY_ACCUMULATED_STEPS, newTotal).apply()
        _todaySteps.value = newTotal
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun updateGoal(newGoal: Int) {
        val validGoal = newGoal.coerceIn(1000, 100000)
        _stepGoal.value = validGoal
        prefs.edit().putInt(KEY_STEP_GOAL, validGoal).apply()
    }

    fun resetTodaySteps() {
        val today = getTodayDateString()
        val lastRaw = prefs.getFloat(KEY_LAST_RAW_STEPS, -1f)
        prefs.edit()
            .putString(KEY_SAVED_DATE, today)
            .putInt(KEY_ACCUMULATED_STEPS, 0)
            .putFloat(KEY_LAST_RAW_STEPS, lastRaw)
            .apply()
        _todaySteps.value = 0
        candidateStreak = 0
        isStreakConfirmed = false
    }
}
