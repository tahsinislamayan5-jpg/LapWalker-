package com.takwa.lapwalker.ui.calisthenics

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.takwa.lapwalker.databinding.ActivityExercisePracticeBinding
import com.takwa.lapwalker.domain.model.calisthenics.ExerciseMetricType
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ExercisePracticeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExercisePracticeBinding
    private val viewModel: ExercisePracticeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExercisePracticeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stepId = intent.getStringExtra("EXTRA_STEP_ID")
        if (stepId != null) {
            viewModel.loadStep(stepId)
        } else {
            finish()
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnToggleGuide.setOnClickListener {
            val isVisible = binding.layoutGuideContent.visibility == View.VISIBLE
            binding.layoutGuideContent.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.btnToggleGuide.text = if (isVisible) "SHOW" else "HIDE"
        }

        binding.btnIncrementRep.setOnClickListener {
            viewModel.incrementRep()
        }

        binding.btnCompleteSet.setOnClickListener {
            viewModel.completeCurrentSet()
        }

        binding.btnStartHold.setOnClickListener {
            viewModel.toggleHoldTimer()
        }

        binding.btnSkipRest.setOnClickListener {
            viewModel.skipRest()
        }

        binding.btnFinishContinue.setOnClickListener {
            finish()
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

    private fun render(state: PracticeUiState) {
        val step = state.step ?: return

        binding.tvExerciseTitle.text = step.name
        binding.chipPillar.text = step.pillar.title

        binding.tvInstructions.text = step.instructions
        binding.tvFormCues.text = step.formCues.joinToString("\n") { "• $it" }

        binding.tvBenchmarkRule.text = "Goal: ${step.unlockBenchmarkText}"

        if (state.isFinished) {
            binding.layoutResultOverlay.visibility = View.VISIBLE
            if (state.wasBenchmarkPassed) {
                binding.tvResultTitle.text = "BENCHMARK PASSED"
                binding.tvResultSubtitle.text = "Next exercise unlocked!"
            } else {
                binding.tvResultTitle.text = "WORKOUT SAVED"
                binding.tvResultSubtitle.text = "Keep practicing to pass the benchmark."
            }
        } else if (state.isResting) {
            binding.layoutResultOverlay.visibility = View.GONE
            binding.layoutRestOverlay.visibility = View.VISIBLE
            binding.tvRestCountdown.text = state.restSecondsRemaining.toString()
        } else {
            binding.layoutResultOverlay.visibility = View.GONE
            binding.layoutRestOverlay.visibility = View.GONE

            if (step.metricType == ExerciseMetricType.REPS) {
                binding.layoutRepsContainer.visibility = View.VISIBLE
                binding.layoutTimerContainer.visibility = View.GONE
                binding.tvSetIndicator.text = "Set ${state.currentSet} of ${step.targetSets}"
                binding.tvRepCounter.text = state.currentReps.toString()
            } else {
                binding.layoutRepsContainer.visibility = View.GONE
                binding.layoutTimerContainer.visibility = View.VISIBLE
                binding.tvSetIndicator.text = "Timed Hold"
                binding.tvHoldTimer.text = state.holdSecondsRemaining.toString()

                binding.progressHold.max = step.targetRepsOrSeconds
                binding.progressHold.progress = step.targetRepsOrSeconds - state.holdSecondsRemaining

                if (state.isHoldRunning) {
                    binding.btnStartHold.text = "PAUSE HOLD"
                } else {
                    binding.btnStartHold.text = "START HOLD"
                }
            }
        }
    }
}
