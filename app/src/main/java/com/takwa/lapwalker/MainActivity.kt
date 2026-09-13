package com.takwa.lapwalker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.takwa.lapwalker.core.constants.AppConstants
import com.takwa.lapwalker.databinding.ActivityMainBinding
import com.takwa.lapwalker.databinding.DialogUpdateAvailableBinding
import com.takwa.lapwalker.domain.model.AppUpdateInfo
import com.takwa.lapwalker.ui.main.MainIntent
import com.takwa.lapwalker.ui.main.MainViewModel
import com.takwa.lapwalker.ui.main.MainViewState
import com.takwa.lapwalker.data.local.db.entity.UserRankProfileEntity
import com.takwa.lapwalker.data.local.db.entity.StreakStatusEntity
import com.takwa.lapwalker.data.local.db.entity.DailyBountyEntity

import com.takwa.lapwalker.ui.main.UpdateStatus
import com.takwa.lapwalker.ui.main.BadgeAdapter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        const val KEY_LAP_FEET = "lap_feet"
        const val KEY_TARGET_KM = "target_km"
        const val KEY_WEIGHT = "weight"
        const val KEY_VIBRATE = "vibrate"
        const val KEY_DARK_THEME = "is_dark_theme"
        private const val REQUEST_CODE_INSTALL_PERMISSION = 1002
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModel()
    private lateinit var historyAdapter: com.takwa.lapwalker.ui.main.adapter.WorkoutHistoryAdapter

    private var currentAppliedTheme: Boolean? = null
    private var isUserTyping = false

    private var pendingInstallApk: File? = null
    private var updateDialog: androidx.appcompat.app.AlertDialog? = null
    private var updateDialogBinding: DialogUpdateAvailableBinding? = null
    private var lastRenderedUpdateStatus: UpdateStatus? = null

    private val requestActivityPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.dispatch(MainIntent.UpdateStepPermission(isGranted))
            if (!isGranted) {
                Toast.makeText(this, "Step tracking requires Physical Activity permission", Toast.LENGTH_SHORT).show()
            }
        }

    private fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        checkOverlayPermissionStatus()

        // Start step sensor if permission is already granted
        val hasStepPerm = hasActivityRecognitionPermission()
        viewModel.dispatch(MainIntent.UpdateStepPermission(hasStepPerm))

        // Display current version dynamically
        binding.tvAppVersionLabel.text = "v${BuildConfig.VERSION_NAME} • Check for Update"

        // Background update check on startup (non-intrusive)
        viewModel.dispatch(MainIntent.CheckForUpdate(isManual = false))
    }

    private fun setupRecyclerView() {
        historyAdapter = com.takwa.lapwalker.ui.main.adapter.WorkoutHistoryAdapter { workout ->
            showDeleteConfirmation(workout)
        }
        binding.rvWorkoutHistory.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvWorkoutHistory.adapter = historyAdapter
    }

    private fun setupListeners() {
        binding.btnTabWalk.setOnClickListener {
            viewModel.dispatch(MainIntent.SelectTab(com.takwa.lapwalker.ui.main.MainTab.WALK))
        }

        binding.btnTabSteps.setOnClickListener {
            viewModel.dispatch(MainIntent.SelectTab(com.takwa.lapwalker.ui.main.MainTab.STEPS))
            if (!hasActivityRecognitionPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestActivityPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        binding.btnTabHistory.setOnClickListener {
            viewModel.dispatch(MainIntent.SelectTab(com.takwa.lapwalker.ui.main.MainTab.HISTORY))
        }

        binding.btnTabWorkouts.setOnClickListener {
            viewModel.dispatch(MainIntent.SelectTab(com.takwa.lapwalker.ui.main.MainTab.WORKOUTS))
        }

        binding.btnGrantStepPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestActivityPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        binding.btnGoal6k.setOnClickListener { viewModel.dispatch(MainIntent.UpdateStepGoal(6000)) }
        binding.btnGoal8k.setOnClickListener { viewModel.dispatch(MainIntent.UpdateStepGoal(8000)) }
        binding.btnGoal10k.setOnClickListener { viewModel.dispatch(MainIntent.UpdateStepGoal(10000)) }
        binding.btnGoal12k.setOnClickListener { viewModel.dispatch(MainIntent.UpdateStepGoal(12000)) }

        binding.btnResetSteps.setOnClickListener {
            showResetStepsConfirmation()
        }

        binding.switchSessionOnlySteps.setOnCheckedChangeListener { _, isChecked ->
            viewModel.dispatch(MainIntent.ToggleSessionOnlySteps(isChecked))
        }

        binding.btnClearHistory.setOnClickListener {
            showClearAllConfirmation()
        }

        binding.btnThemeToggle.setOnClickListener {
            viewModel.dispatch(MainIntent.ToggleTheme)
        }

        binding.btnCheckUpdate.setOnClickListener {
            viewModel.dispatch(MainIntent.CheckForUpdate(isManual = true))
        }

        binding.root.findViewById<View>(R.id.ll_division_cockpit)?.setOnClickListener {
            showDivisionRoadmapDialog()
        }
        binding.root.findViewById<View>(R.id.ll_streak_cockpit)?.setOnClickListener {
            showStreakDetailsDialog()
        }
        binding.root.findViewById<View>(R.id.ll_bounties_cockpit)?.setOnClickListener {
            showBountiesAndQuestsDialog()
        }
        binding.root.findViewById<View>(R.id.ll_badges_cockpit)?.setOnClickListener {
            showBadgesDialog()
        }

        binding.etLapLength.doAfterTextChanged { text ->
            if (isUserTyping) {
                val feet = text.toString().trim().toFloatOrNull() ?: 0f
                viewModel.dispatch(MainIntent.UpdateLapFeet(feet))
            }
        }

        binding.etTargetKm.doAfterTextChanged { text ->
            if (isUserTyping) {
                val target = text.toString().trim().toFloatOrNull() ?: 0f
                viewModel.dispatch(MainIntent.UpdateTargetKm(target))
            }
        }

        binding.etWeight.doAfterTextChanged { text ->
            if (isUserTyping) {
                val weight = text.toString().trim().toFloatOrNull() ?: 0f
                viewModel.dispatch(MainIntent.UpdateWeight(weight))
            }
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            if (isUserTyping) {
                viewModel.dispatch(MainIntent.ToggleVibration(isChecked))
            }
        }

        binding.btnStartWalking.setOnClickListener {
            if (checkAndRequestOverlayPermission()) {
                startWalkingService()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: MainViewState) {
        // Sync input fields if not currently being actively typed by user
        if (!isUserTyping) {
            val currentLapText = binding.etLapLength.text.toString()
            if (currentLapText != state.lapFeet.toString() && currentLapText.isEmpty()) {
                binding.etLapLength.setText(state.lapFeet.toString())
            }

            val currentTargetText = binding.etTargetKm.text.toString()
            if (currentTargetText != state.targetKm.toString() && currentTargetText.isEmpty()) {
                binding.etTargetKm.setText(state.targetKm.toString())
            }

            val currentWeightText = binding.etWeight.text.toString()
            if (currentWeightText != state.weightKg.toString() && currentWeightText.isEmpty()) {
                binding.etWeight.setText(state.weightKg.toString())
            }

            binding.switchVibration.isChecked = state.vibrateEnabled
            isUserTyping = true
        }

        // Render Live Target Preview
        binding.tvPreviewKm.text = String.format(Locale.US, "%.2f km", state.targetKm)
        binding.tvPreviewLapsSub.text = String.format(
            Locale.US,
            "%d laps (%.0f ft/lap)",
            state.requiredLaps,
            state.lapFeet
        )
        binding.tvPreviewCalories.text = String.format(Locale.US, "~%.0f kcal", state.estCalories)

        // Render Permission Banner
        if (state.hasOverlayPermission) {
            binding.tvPermissionHint.text = "✓ Ready! Bubble will show over your screen when started."
            binding.tvPermissionHint.setTextColor(0xFF059669.toInt())
        } else {
            binding.tvPermissionHint.text = "⚠️ Needs 'Display over other apps' permission to show bubble over manhwa."
            binding.tvPermissionHint.setTextColor(0xFFD97706.toInt())
        }

        // Render Steps Tab Data
        val stepState = state.stepState
        val needsStepPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityRecognitionPermission()
        binding.cardStepPermission.visibility = if (needsStepPerm) View.VISIBLE else View.GONE

        binding.tvTodaySteps.text = String.format(Locale.US, "%,d", stepState.todaySteps)
        binding.tvStepGoalSub.text = String.format(Locale.US, "/ %,d goal", stepState.stepGoal)
        binding.progressStepRing.progress = stepState.progressPercent
        binding.tvStepPercentBadge.text = if (stepState.isGoalReached) "🎉 GOAL REACHED!" else "${stepState.progressPercent}% COMPLETED"

        binding.tvStepDistance.text = String.format(Locale.US, "%.2f km", stepState.distanceKm)
        binding.tvStepCalories.text = String.format(Locale.US, "%.0f kcal", stepState.caloriesBurned)
        binding.tvStepTime.text = "${stepState.activeMinutes} min"

        if (binding.switchSessionOnlySteps.isChecked != stepState.isSessionOnlyMode) {
            binding.switchSessionOnlySteps.isChecked = stepState.isSessionOnlyMode
        }

        // Highlight active goal button
        val pillActiveBg = if (state.isDarkTheme) R.drawable.tab_pill_active_neon else R.drawable.tab_pill_active_light
        val pillInactiveBg = if (state.isDarkTheme) R.drawable.theme_chip_bg_dark else R.drawable.theme_chip_bg_light
        val pillActiveText = if (state.isDarkTheme) 0xFF0B0F19.toInt() else 0xFFFFFFFF.toInt()
        val pillInactiveText = if (state.isDarkTheme) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()

        val goals = listOf(
            Pair(binding.btnGoal6k, 6000),
            Pair(binding.btnGoal8k, 8000),
            Pair(binding.btnGoal10k, 10000),
            Pair(binding.btnGoal12k, 12000)
        )
        for ((btn, goalVal) in goals) {
            val isSelected = stepState.stepGoal == goalVal
            btn.setBackgroundResource(if (isSelected) pillActiveBg else pillInactiveBg)
            btn.setTextColor(if (isSelected) pillActiveText else pillInactiveText)
        }

        // Render Tabs & History
        renderTabs(state.selectedTab, state.isDarkTheme)

        renderCalisthenicsSteps(state.calisthenicsProgress)

        // Render History Data
        historyAdapter.submitList(state.workouts)
        if (state.workouts.isEmpty()) {
            binding.layoutEmptyHistory.visibility = View.VISIBLE
            binding.rvWorkoutHistory.visibility = View.GONE
            binding.btnClearHistory.visibility = View.GONE
        } else {
            binding.layoutEmptyHistory.visibility = View.GONE
            binding.rvWorkoutHistory.visibility = View.VISIBLE
            binding.btnClearHistory.visibility = View.VISIBLE
        }

        // Render Lifetime Summary
        binding.tvLifeDistance.text = String.format(Locale.US, "%.2f km", state.totalLifetimeDistanceKm)
        binding.tvLifeLaps.text = String.format(Locale.US, "%d laps", state.totalLifetimeLaps)
        val totalMins = state.totalLifetimeDurationSeconds / 60
        binding.tvLifeTime.text = if (totalMins >= 60) {
            val hours = totalMins / 60
            val remMins = totalMins % 60
            String.format(Locale.US, "%dh %dm", hours, remMins)
        } else {
            String.format(Locale.US, "%dm", totalMins)
        }
        binding.tvLifeCalories.text = String.format(Locale.US, "%.0f kcal", state.totalLifetimeCalories)

        // Render Theme if changed
        if (currentAppliedTheme != state.isDarkTheme) {
            currentAppliedTheme = state.isDarkTheme
            applyTheme(state.isDarkTheme)
        }

        handleUpdateStatus(state.updateStatus)

        // Gamification
        renderGamification(state)
    }

    private fun renderCalisthenicsSteps(progressList: List<com.takwa.lapwalker.data.local.db.entity.CalisthenicsProgressEntity>) {
        val catalog = com.takwa.lapwalker.data.catalog.CalisthenicsCatalog
        val progressMap = progressList.associateBy { it.stepId }

        fun populatePillar(pillar: com.takwa.lapwalker.domain.model.calisthenics.CalisthenicsPillar, container: android.widget.LinearLayout) {
            container.removeAllViews()
            val steps = catalog.getStepsForPillar(pillar)
            for (step in steps) {
                val progress = progressMap[step.id]
                val view = layoutInflater.inflate(R.layout.item_calisthenics_step, container, false)

                val tvStepNum = view.findViewById<android.widget.TextView>(R.id.tv_step_number)
                val tvStepName = view.findViewById<android.widget.TextView>(R.id.tv_step_name)
                val tvStepStatus = view.findViewById<android.widget.TextView>(R.id.tv_step_status)
                val btnAction = view.findViewById<android.widget.Button>(R.id.btn_action)

                tvStepNum.text = step.stepNumber.toString()
                tvStepName.text = step.name

                if (progress?.isUnlocked == true) {
                    if (progress.isMastered) {
                        tvStepStatus.visibility = View.VISIBLE
                        tvStepStatus.text = "Mastered"
                        tvStepStatus.setTextColor(0xFF10B981.toInt())
                        btnAction.text = "PRACTICE"
                        btnAction.setBackgroundColor(0xFF0F172A.toInt())
                        btnAction.setTextColor(0xFFFFFFFF.toInt())
                    } else {
                        tvStepStatus.visibility = View.GONE
                        btnAction.text = "START"
                        btnAction.setBackgroundColor(0xFF0284C7.toInt())
                        btnAction.setTextColor(0xFFFFFFFF.toInt())
                    }

                    btnAction.setOnClickListener {
                        val intent = Intent(this, com.takwa.lapwalker.ui.calisthenics.ExercisePracticeActivity::class.java).apply {
                            putExtra("EXTRA_STEP_ID", step.id)
                        }
                        startActivity(intent)
                    }
                } else {
                    val reqLevel = when (step.id) {
                        "legs_2" -> 5
                        "core_2" -> 10
                        "push_2" -> 15
                        "pull_2" -> 20
                        "legs_3" -> 25
                        "core_3" -> 30
                        "push_3" -> 35
                        "pull_3" -> 40
                        "push_4" -> 45
                        "push_5" -> 49
                        else -> 1
                    }
                    tvStepStatus.visibility = View.VISIBLE
                    tvStepStatus.text = "Unlocks at Lv $reqLevel"
                    tvStepStatus.setTextColor(0xFFEF4444.toInt())
                    btnAction.text = "🔒"
                    btnAction.setBackgroundColor(0xFF64748B.toInt())
                    btnAction.setTextColor(0xFFFFFFFF.toInt())
                    btnAction.setOnClickListener(null)
                    tvStepNum.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF64748B.toInt())
                }

                container.addView(view)
            }
        }

        populatePillar(com.takwa.lapwalker.domain.model.calisthenics.CalisthenicsPillar.PUSH, binding.layoutPushSteps)
        populatePillar(com.takwa.lapwalker.domain.model.calisthenics.CalisthenicsPillar.PULL, binding.layoutPullSteps)
        populatePillar(com.takwa.lapwalker.domain.model.calisthenics.CalisthenicsPillar.LEGS, binding.layoutLegsSteps)
        populatePillar(com.takwa.lapwalker.domain.model.calisthenics.CalisthenicsPillar.CORE, binding.layoutCoreSteps)
    }

    private fun renderGamification(state: MainViewState) {
        state.rankProfile?.let { profile ->
            findViewById<android.widget.TextView>(com.takwa.lapwalker.R.id.tv_division_name)?.text = "${profile.divisionName} • LVL ${profile.currentLevel}"
            findViewById<android.widget.TextView>(com.takwa.lapwalker.R.id.tv_xp_progress)?.text = "${profile.totalXp % 1000} / 1000 XP (Total: ${profile.totalXp})"
        }

        state.streakStatus?.let { streak ->
            val flameEmoji = when (streak.flameTier) {
                "EMBER" -> "🔥"
                "VIBRANT AMBER" -> "🧡"
                "COBALT BLAZE" -> "💙"
                "SOLAR RADIANCE" -> "✨"
                else -> "💨"
            }
            findViewById<android.widget.TextView>(com.takwa.lapwalker.R.id.tv_flame_streak)?.text = "$flameEmoji ${streak.currentStreakDays} DAYS • ${streak.flameTier}"
            findViewById<android.widget.TextView>(com.takwa.lapwalker.R.id.tv_shields)?.text = "🛡️ ${streak.freezeShieldsAvailable}/2  |  🪙 ${streak.restTokensAvailable}/2"
        }

        findViewById<android.widget.TextView>(com.takwa.lapwalker.R.id.tv_bounties_summary)?.text = "${state.bounties.count { it.isCompleted }}/${state.bounties.size} Completed"
        findViewById<android.widget.TextView>(com.takwa.lapwalker.R.id.tv_badges_summary)?.text = "${state.badges.count { it.isUnlocked }}/${state.badges.size} Unlocked"
    }

    private fun renderTabs(selectedTab: com.takwa.lapwalker.ui.main.MainTab, isDark: Boolean) {
        val activeBg = if (isDark) R.drawable.tab_pill_active_neon else R.drawable.tab_pill_active_light
        val activeTextColor = if (isDark) 0xFF0B0F19.toInt() else 0xFFFFFFFF.toInt()
        val inactiveTextColor = if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()

        binding.layoutWalkTab.visibility = if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.WALK) View.VISIBLE else View.GONE
        binding.layoutStepsTab.visibility = if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.STEPS) View.VISIBLE else View.GONE
        binding.layoutHistoryTab.visibility = if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.HISTORY) View.VISIBLE else View.GONE
        binding.layoutWorkoutsTab.visibility = if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.WORKOUTS) View.VISIBLE else View.GONE

        binding.btnTabWalk.setBackgroundResource(if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.WALK) activeBg else 0)
        binding.btnTabWalk.setTextColor(if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.WALK) activeTextColor else inactiveTextColor)

        binding.btnTabSteps.setBackgroundResource(if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.STEPS) activeBg else 0)
        binding.btnTabSteps.setTextColor(if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.STEPS) activeTextColor else inactiveTextColor)

        binding.btnTabHistory.setBackgroundResource(if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.HISTORY) activeBg else 0)
        binding.btnTabHistory.setTextColor(if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.HISTORY) activeTextColor else inactiveTextColor)

        binding.btnTabWorkouts.setBackgroundResource(if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.WORKOUTS) activeBg else 0)
        binding.btnTabWorkouts.setTextColor(if (selectedTab == com.takwa.lapwalker.ui.main.MainTab.WORKOUTS) activeTextColor else inactiveTextColor)
    }

    private fun showResetStepsConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Today's Steps")
            .setMessage("Reset step counter for today back to 0?")
            .setPositiveButton("Reset") { _, _ ->
                viewModel.dispatch(MainIntent.ResetTodaySteps)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(workout: com.takwa.lapwalker.domain.model.WorkoutRecord) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Workout")
            .setMessage("Delete workout record for ${String.format(Locale.US, "%.2f km (%d laps)", workout.distanceKm, workout.laps)}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.dispatch(MainIntent.DeleteWorkout(workout.id))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearAllConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear All History")
            .setMessage("Are you sure you want to permanently clear all workout sessions?")
            .setPositiveButton("Clear All") { _, _ ->
                viewModel.dispatch(MainIntent.ClearAllWorkouts)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleUpdateStatus(status: UpdateStatus) {
        if (lastRenderedUpdateStatus == status) return
        lastRenderedUpdateStatus = status

        when (status) {
            is UpdateStatus.Idle -> {
                updateDialog?.dismiss()
                updateDialog = null
                updateDialogBinding = null
            }
            is UpdateStatus.Checking -> {
                if (status.isManual) {
                    Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
                }
            }
            is UpdateStatus.UpToDate -> {
                if (status.isManual) {
                    Toast.makeText(this, "LapWalker is up to date! (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                }
            }
            is UpdateStatus.Error -> {
                if (status.isManual) {
                    Toast.makeText(this, "Update check: ${status.message}", Toast.LENGTH_SHORT).show()
                }
            }
            is UpdateStatus.UpdateAvailable -> {
                showUpdateDialog(status.updateInfo)
            }
            is UpdateStatus.Downloading -> {
                renderDownloadProgress(status.percent, status.downloaded, status.total)
            }
            is UpdateStatus.ReadyToInstall -> {
                updateDialog?.dismiss()
                updateDialog = null
                updateDialogBinding = null
                installApk(status.apkFile)
            }
        }
    }

    private fun showUpdateDialog(info: AppUpdateInfo) {
        if (updateDialog?.isShowing == true) return

        val dialogBinding = DialogUpdateAvailableBinding.inflate(layoutInflater)
        updateDialogBinding = dialogBinding

        val sizeText = if (info.fileSizeBytes > 0) {
            String.format(Locale.US, "%.1f MB", info.fileSizeBytes / (1024f * 1024f))
        } else {
            "New Release"
        }
        dialogBinding.tvUpdateVersion.text = "v${info.versionName} • $sizeText"
        dialogBinding.tvChangelog.text = info.changelog.ifBlank { "Performance improvements and bug fixes." }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .setOnCancelListener {
                viewModel.dispatch(MainIntent.DismissUpdateDialog)
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        updateDialog = dialog

        dialogBinding.btnLater.setOnClickListener {
            dialog.dismiss()
            viewModel.dispatch(MainIntent.DismissUpdateDialog)
        }

        dialogBinding.btnUpdateNow.setOnClickListener {
            dialogBinding.btnUpdateNow.isEnabled = false
            dialogBinding.btnLater.isEnabled = false
            dialogBinding.layoutDownloadProgress.visibility = View.VISIBLE
            viewModel.dispatch(MainIntent.StartDownloadUpdate(info.downloadUrl))
        }

        dialog.show()
    }

    private fun renderDownloadProgress(percent: Int, downloaded: Long, total: Long) {
        val dialogBinding = updateDialogBinding ?: return
        dialogBinding.layoutDownloadProgress.visibility = View.VISIBLE
        dialogBinding.btnUpdateNow.isEnabled = false
        dialogBinding.btnLater.isEnabled = false

        if (percent >= 0) {
            dialogBinding.progressBarDownload.isIndeterminate = false
            dialogBinding.progressBarDownload.progress = percent
            val downloadedMb = downloaded / (1024f * 1024f)
            val totalMb = total / (1024f * 1024f)
            dialogBinding.tvProgressText.text = String.format(Locale.US, "Downloading: %d%% (%.1f / %.1f MB)", percent, downloadedMb, totalMb)
        } else {
            dialogBinding.progressBarDownload.isIndeterminate = true
            val downloadedMb = downloaded / (1024f * 1024f)
            dialogBinding.tvProgressText.text = String.format(Locale.US, "Downloading... (%.1f MB)", downloadedMb)
        }
    }

    private fun installApk(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                pendingInstallApk = apkFile
                Toast.makeText(this, "Please allow LapWalker to install updates", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, REQUEST_CODE_INSTALL_PERMISSION)
                return
            }
        }
        launchInstallIntent(apkFile)
    }

    private fun launchInstallIntent(apkFile: File) {
        try {
            val installIntent = viewModel.updateRepository.getInstallIntent(apkFile)
            startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyTheme(dark: Boolean) {
        if (dark) {
            // Dark Mode Theme
            binding.mainScrollRoot.setBackgroundColor(0xFF0B0F19.toInt())
            binding.tvMainTitle.setTextColor(0xFF00E5FF.toInt())
            binding.tvMainSubtitle.setTextColor(0xFF94A3B8.toInt())

            binding.btnThemeToggle.setBackgroundResource(R.drawable.theme_chip_bg_dark)
            binding.tvThemeIcon.text = "🌙"
            binding.tvThemeLabel.text = "DARK"
            binding.tvThemeLabel.setTextColor(0xFFFFFFFF.toInt())

            binding.btnCheckUpdate.setBackgroundResource(R.drawable.theme_chip_bg_dark)
            binding.tvAppVersionLabel.setTextColor(0xFF94A3B8.toInt())

            binding.cardHowTo.setBackgroundResource(R.drawable.card_background)
            binding.tvHowtoTitle.setTextColor(0xFF00E5FF.toInt())
            binding.tvHowtoBody.setTextColor(0xFFE2E8F0.toInt())

            binding.cardConfig.setBackgroundResource(R.drawable.card_background)
            binding.tvConfigTitle.setTextColor(0xFF00E5FF.toInt())
            binding.tvLabelLap.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelTarget.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelWeight.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelVibrate.setTextColor(0xFFE2E8F0.toInt())

            val darkInputBg = R.drawable.stat_card_bg
            binding.etLapLength.setBackgroundResource(darkInputBg)
            binding.etLapLength.setTextColor(0xFFFFFFFF.toInt())
            binding.etLapLength.setHintTextColor(0xFF64748B.toInt())

            binding.etTargetKm.setBackgroundResource(darkInputBg)
            binding.etTargetKm.setTextColor(0xFFFFFFFF.toInt())
            binding.etTargetKm.setHintTextColor(0xFF64748B.toInt())

            binding.etWeight.setBackgroundResource(darkInputBg)
            binding.etWeight.setTextColor(0xFFFFFFFF.toInt())
            binding.etWeight.setHintTextColor(0xFF64748B.toInt())

            binding.cardPreview.setBackgroundResource(R.drawable.card_background)
            binding.tvPreviewTitle.setTextColor(0xFF00E5FF.toInt())
            binding.boxPreviewLaps.setBackgroundResource(darkInputBg)
            binding.boxPreviewCal.setBackgroundResource(darkInputBg)
            binding.tvLabelReqKm.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelEstCal.setTextColor(0xFF94A3B8.toInt())
            binding.tvPreviewKm.setTextColor(0xFF00E5FF.toInt())
            binding.tvPreviewLapsSub.setTextColor(0xFF64748B.toInt())
            binding.tvPreviewCalories.setTextColor(0xFFFB923C.toInt())

            binding.btnStartWalking.setBackgroundResource(R.drawable.btn_neon_gradient)
            binding.btnStartWalking.setTextColor(0xFF0F172A.toInt())

            // History Tab Theming (Dark)
            historyAdapter.setDarkTheme(true)
            binding.layoutTabBar.setBackgroundResource(R.drawable.tab_segment_bg_dark)
            binding.cardLifetimeStats.setBackgroundResource(R.drawable.card_background)
            binding.tvLifetimeTitle.setTextColor(0xFF00E5FF.toInt())
            binding.boxLifeDist.setBackgroundResource(darkInputBg)
            binding.boxLifeLaps.setBackgroundResource(darkInputBg)
            binding.boxLifeTime.setBackgroundResource(darkInputBg)
            binding.boxLifeCal.setBackgroundResource(darkInputBg)
            binding.tvLabelLifeDist.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelLifeLaps.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelLifeTime.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelLifeCal.setTextColor(0xFF94A3B8.toInt())
            binding.tvLifeDistance.setTextColor(0xFF00E5FF.toInt())
            binding.tvLifeLaps.setTextColor(0xFF00E5FF.toInt())
            binding.tvLifeTime.setTextColor(0xFF00E5FF.toInt())
            binding.tvLifeCalories.setTextColor(0xFFFB923C.toInt())
            binding.tvHistoryHeader.setTextColor(0xFF00E5FF.toInt())
            binding.tvEmptyTitle.setTextColor(0xFFFFFFFF.toInt())
            binding.tvEmptySubtitle.setTextColor(0xFF94A3B8.toInt())

            // Steps Tab Theming (Dark)
            binding.cardStepPermission.setBackgroundResource(R.drawable.card_background)
            binding.cardStepGauge.setBackgroundResource(R.drawable.card_background)
            binding.cardStepGoalSettings.setBackgroundResource(R.drawable.card_background)
            binding.progressStepRing.progressDrawable = ContextCompat.getDrawable(this, R.drawable.step_progress_ring)
            binding.tvTodaySteps.setTextColor(0xFFFFFFFF.toInt())
            binding.tvStepsTodayLabel.setTextColor(0xFF94A3B8.toInt())
            binding.tvStepGoalSub.setTextColor(0xFF94A3B8.toInt())
            binding.tvStepPercentBadge.setBackgroundResource(R.drawable.theme_chip_bg_dark)
            binding.tvStepPercentBadge.setTextColor(0xFF00E5FF.toInt())
            binding.tvStepGoalHeader.setTextColor(0xFF00E5FF.toInt())

            binding.boxStepDist.setBackgroundResource(darkInputBg)
            binding.boxStepCal.setBackgroundResource(darkInputBg)
            binding.boxStepTime.setBackgroundResource(darkInputBg)
            binding.tvLabelStepDist.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelStepCal.setTextColor(0xFF94A3B8.toInt())
            binding.tvLabelStepTime.setTextColor(0xFF94A3B8.toInt())
            binding.tvStepDistance.setTextColor(0xFF00E5FF.toInt())
            binding.tvStepCalories.setTextColor(0xFFFB923C.toInt())
            binding.tvStepTime.setTextColor(0xFF10B981.toInt())
            binding.tvSensorStatusLabel.setTextColor(0xFF10B981.toInt())
            binding.cardStepFilterConfig.setBackgroundResource(R.drawable.card_background)
            binding.tvSessionOnlyTitle.setTextColor(0xFF00E5FF.toInt())
            binding.tvSessionOnlyDesc.setTextColor(0xFF94A3B8.toInt())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = 0xFF0B0F19.toInt()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val decor = window.decorView
                decor.systemUiVisibility = decor.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        } else {
            // Light Mode Theme (Default)
            binding.mainScrollRoot.setBackgroundColor(0xFFF1F5F9.toInt())
            binding.tvMainTitle.setTextColor(0xFF0284C7.toInt())
            binding.tvMainSubtitle.setTextColor(0xFF64748B.toInt())

            binding.btnThemeToggle.setBackgroundResource(R.drawable.theme_chip_bg_light)
            binding.tvThemeIcon.text = "☀️"
            binding.tvThemeLabel.text = "LIGHT"
            binding.tvThemeLabel.setTextColor(0xFF0F172A.toInt())

            binding.btnCheckUpdate.setBackgroundResource(R.drawable.theme_chip_bg_light)
            binding.tvAppVersionLabel.setTextColor(0xFF64748B.toInt())

            binding.cardHowTo.setBackgroundResource(R.drawable.card_background_light)
            binding.tvHowtoTitle.setTextColor(0xFF0284C7.toInt())
            binding.tvHowtoBody.setTextColor(0xFF334155.toInt())

            binding.cardConfig.setBackgroundResource(R.drawable.card_background_light)
            binding.tvConfigTitle.setTextColor(0xFF0284C7.toInt())
            binding.tvLabelLap.setTextColor(0xFF64748B.toInt())
            binding.tvLabelTarget.setTextColor(0xFF64748B.toInt())
            binding.tvLabelWeight.setTextColor(0xFF64748B.toInt())
            binding.tvLabelVibrate.setTextColor(0xFF334155.toInt())

            val lightInputBg = R.drawable.stat_card_bg_light
            binding.etLapLength.setBackgroundResource(lightInputBg)
            binding.etLapLength.setTextColor(0xFF0F172A.toInt())
            binding.etLapLength.setHintTextColor(0xFF94A3B8.toInt())

            binding.etTargetKm.setBackgroundResource(lightInputBg)
            binding.etTargetKm.setTextColor(0xFF0F172A.toInt())
            binding.etTargetKm.setHintTextColor(0xFF94A3B8.toInt())

            binding.etWeight.setBackgroundResource(lightInputBg)
            binding.etWeight.setTextColor(0xFF0F172A.toInt())
            binding.etWeight.setHintTextColor(0xFF94A3B8.toInt())

            binding.cardPreview.setBackgroundResource(R.drawable.card_background_light)
            binding.tvPreviewTitle.setTextColor(0xFF0284C7.toInt())
            binding.boxPreviewLaps.setBackgroundResource(lightInputBg)
            binding.boxPreviewCal.setBackgroundResource(lightInputBg)
            binding.tvLabelReqKm.setTextColor(0xFF64748B.toInt())
            binding.tvLabelEstCal.setTextColor(0xFF64748B.toInt())
            binding.tvPreviewKm.setTextColor(0xFF0284C7.toInt())
            binding.tvPreviewLapsSub.setTextColor(0xFF64748B.toInt())
            binding.tvPreviewCalories.setTextColor(0xFFEA580C.toInt())

            binding.btnStartWalking.setBackgroundResource(R.drawable.btn_blue_gradient)
            binding.btnStartWalking.setTextColor(0xFFFFFFFF.toInt())

            // History Tab Theming (Light)
            historyAdapter.setDarkTheme(false)
            binding.layoutTabBar.setBackgroundResource(R.drawable.tab_segment_bg_light)
            binding.cardLifetimeStats.setBackgroundResource(R.drawable.card_background_light)
            binding.tvLifetimeTitle.setTextColor(0xFF0284C7.toInt())
            binding.boxLifeDist.setBackgroundResource(lightInputBg)
            binding.boxLifeLaps.setBackgroundResource(lightInputBg)
            binding.boxLifeTime.setBackgroundResource(lightInputBg)
            binding.boxLifeCal.setBackgroundResource(lightInputBg)
            binding.tvLabelLifeDist.setTextColor(0xFF64748B.toInt())
            binding.tvLabelLifeLaps.setTextColor(0xFF64748B.toInt())
            binding.tvLabelLifeTime.setTextColor(0xFF64748B.toInt())
            binding.tvLabelLifeCal.setTextColor(0xFF64748B.toInt())
            binding.tvLifeDistance.setTextColor(0xFF0284C7.toInt())
            binding.tvLifeLaps.setTextColor(0xFF0284C7.toInt())
            binding.tvLifeTime.setTextColor(0xFF0284C7.toInt())
            binding.tvLifeCalories.setTextColor(0xFFEA580C.toInt())
            binding.tvHistoryHeader.setTextColor(0xFF0284C7.toInt())
            binding.tvEmptyTitle.setTextColor(0xFF0F172A.toInt())
            binding.tvEmptySubtitle.setTextColor(0xFF64748B.toInt())

            // Steps Tab Theming (Light)
            binding.cardStepPermission.setBackgroundResource(R.drawable.card_background_light)
            binding.cardStepGauge.setBackgroundResource(R.drawable.card_background_light)
            binding.cardStepGoalSettings.setBackgroundResource(R.drawable.card_background_light)
            binding.progressStepRing.progressDrawable = ContextCompat.getDrawable(this, R.drawable.step_progress_ring_light)
            binding.tvTodaySteps.setTextColor(0xFF0F172A.toInt())
            binding.tvStepsTodayLabel.setTextColor(0xFF64748B.toInt())
            binding.tvStepGoalSub.setTextColor(0xFF64748B.toInt())
            binding.tvStepPercentBadge.setBackgroundResource(R.drawable.theme_chip_bg_light)
            binding.tvStepPercentBadge.setTextColor(0xFF0284C7.toInt())
            binding.tvStepGoalHeader.setTextColor(0xFF0284C7.toInt())

            binding.boxStepDist.setBackgroundResource(lightInputBg)
            binding.boxStepCal.setBackgroundResource(lightInputBg)
            binding.boxStepTime.setBackgroundResource(lightInputBg)
            binding.tvLabelStepDist.setTextColor(0xFF64748B.toInt())
            binding.tvLabelStepCal.setTextColor(0xFF64748B.toInt())
            binding.tvLabelStepTime.setTextColor(0xFF64748B.toInt())
            binding.tvStepDistance.setTextColor(0xFF0284C7.toInt())
            binding.tvStepCalories.setTextColor(0xFFEA580C.toInt())
            binding.tvStepTime.setTextColor(0xFF10B981.toInt())
            binding.tvSensorStatusLabel.setTextColor(0xFF10B981.toInt())
            binding.cardStepFilterConfig.setBackgroundResource(R.drawable.card_background_light)
            binding.tvSessionOnlyTitle.setTextColor(0xFF0F172A.toInt())
            binding.tvSessionOnlyDesc.setTextColor(0xFF64748B.toInt())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = 0xFFF1F5F9.toInt()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val decor = window.decorView
                decor.systemUiVisibility = decor.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun checkOverlayPermissionStatus(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
        viewModel.dispatch(MainIntent.UpdatePermissionStatus(hasPermission))
        return hasPermission
    }

    private fun checkAndRequestOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please enable 'Display over other apps' for LapWalker", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, AppConstants.OVERLAY_PERMISSION_REQ_CODE)
                return false
            }
        }
        return true
    }

    private fun startWalkingService() {
        val state = viewModel.uiState.value

        val serviceIntent = Intent(this, LapOverlayService::class.java).apply {
            putExtra(KEY_LAP_FEET, state.lapFeet)
            putExtra(KEY_TARGET_KM, state.targetKm)
            putExtra(KEY_WEIGHT, state.weightKg)
            putExtra(KEY_VIBRATE, state.vibrateEnabled)
            putExtra(KEY_DARK_THEME, state.isDarkTheme)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Lap bubble started at 0 laps! Open your manhwa and walk.", Toast.LENGTH_SHORT).show()

        // Minimize app to let user open manhwa reader immediately
        moveTaskToBack(true)
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermissionStatus()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppConstants.OVERLAY_PERMISSION_REQ_CODE) {
            if (checkOverlayPermissionStatus()) {
                Toast.makeText(this, "Permission granted! Tap Start Walking.", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_CODE_INSTALL_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
                pendingInstallApk?.let { launchInstallIntent(it) }
            } else {
                Toast.makeText(this, "Permission not granted to install update.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDivisionRoadmapDialog() {
        val profile = viewModel.uiState.value.rankProfile ?: return
        val currentLvl = profile.currentLevel
        val currentXp = profile.totalXp
        val xpInLevel = currentXp % 1000

        val message = StringBuilder()
        message.append("Current Division: ${profile.divisionName}\n")
        message.append("Rank Level: $currentLvl / 50\n")
        message.append("Total XP: $currentXp ($xpInLevel / 1000 in Level)\n\n")
        message.append("Division Progression Roadmap:\n")
        message.append("• Stage 1: Recruit (Lv 1-4) - Base 4 steps\n")
        message.append("• Stage 2: Iron Scout (Lv 5-9) - Air Squats\n")
        message.append("• Stage 3: Iron Specialist (Lv 10-14) - Hollow Body\n")
        message.append("• Stage 4: Steel Operative (Lv 15-19) - Incline Push-ups\n")
        message.append("• Stage 5: Steel Vanguard (Lv 20-24) - Towel Rows\n")
        message.append("• Stage 6: Mithril Veteran (Lv 25-29) - Split Squats (Legs Mastered)\n")
        message.append("• Stage 7: Mithril Sentinel (Lv 30-34) - Knee Raises (Core Mastered)\n")
        message.append("• Stage 8: Adamant Juggernaut (Lv 35-39) - Low Incline Push-ups\n")
        message.append("• Stage 9: Adamant Commander (Lv 40-44) - Doorframe Rows (Pull Mastered)\n")
        message.append("• Stage 9.5: Pre-Apex (Lv 45-49) - Military Floor Push-ups (Push Mastered)\n")
        message.append("• Stage 10: Apex Immortal (Lv 50) - Living Defense System")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🏆 Ranked Divisions Roadmap")
            .setMessage(message.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showStreakDetailsDialog() {
        val streak = viewModel.uiState.value.streakStatus ?: return
        val message = StringBuilder()
        message.append("Current Streak: ${streak.currentStreakDays} Days\n")
        message.append("Longest Streak: ${streak.longestStreakDays} Days\n")
        message.append("Living Flame Tier: ${streak.flameTier}\n\n")
        message.append("🛡️ Freeze Shields: ${streak.freezeShieldsAvailable} / 2\n")
        message.append("🪙 Rest Tokens: ${streak.restTokensAvailable} / 2\n\n")
        message.append("Flame Tier Milestones:\n")
        message.append("• Days 1-6: 🔥 Ember\n")
        message.append("• Days 7-29: 🧡 Vibrant Amber\n")
        message.append("• Days 30-99: 💙 Cobalt Blaze\n")
        message.append("• Days 100+: ✨ Solar Radiance\n\n")
        message.append("Protect your flame by walking at least 1 lap or doing 1 calisthenics set daily!")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔥 Living Flame Streak Engine")
            .setMessage(message.toString())
            .setPositiveButton("Keep Flaming", null)
            .show()
    }

    private fun showBountiesAndQuestsDialog() {
        val state = viewModel.uiState.value
        val bounties = state.bounties
        val quests = state.quests

        val message = StringBuilder()
        message.append("DAILY BOUNTIES:\n")
        if (bounties.isEmpty()) {
            message.append("• No bounties active today.\n")
        } else {
            for (b in bounties) {
                val icon = if (b.isCompleted) "✓" else "○"
                val progress = if (b.targetType == "DISTANCE_METERS") {
                    String.format(Locale.US, "%.0fm / %.0fm", b.currentProgress, b.targetValue)
                } else {
                    String.format(Locale.US, "%.0f / %.0f", b.currentProgress, b.targetValue)
                }
                message.append("$icon ${b.title} (+${b.xpReward} XP) - $progress\n")
            }
        }

        message.append("\nOPTIONAL ARMORY QUEST:\n")
        if (quests.isEmpty()) {
            message.append("• No armory quest today.\n")
        } else {
            for (q in quests) {
                val status = if (q.isCompleted) "[COMPLETED]" else "[TAP TO CLAIM]"
                message.append("• ${q.title} ($status)\n  ${q.description} -> Reward: +1 ${q.rewardType}\n")
            }
        }

        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎯 Bounties & Armory Quests")
            .setMessage(message.toString())
            .setPositiveButton("Done", null)

        val uncompletedQuest = quests.find { !it.isCompleted }
        if (uncompletedQuest != null) {
            dialogBuilder.setNeutralButton("Claim Quest") { _, _ ->
                viewModel.dispatch(MainIntent.CompleteQuest(uncompletedQuest.id))
            }
        }

        dialogBuilder.show()
    }

    private fun showBadgesDialog() {
        val badges = viewModel.uiState.value.badges
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_badges, null)
        val rvBadges = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_badges)
        rvBadges.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvBadges.adapter = BadgeAdapter(badges)
        dialog.setContentView(view)
        dialog.show()
    }
}
