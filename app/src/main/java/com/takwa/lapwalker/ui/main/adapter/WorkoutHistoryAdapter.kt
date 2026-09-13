package com.takwa.lapwalker.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.takwa.lapwalker.R
import com.takwa.lapwalker.databinding.ItemWorkoutHistoryBinding
import com.takwa.lapwalker.domain.model.WorkoutRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutHistoryAdapter(
    private val onDeleteClick: (WorkoutRecord) -> Unit
) : ListAdapter<WorkoutRecord, WorkoutHistoryAdapter.WorkoutViewHolder>(WorkoutDiffCallback) {

    private var isDarkTheme: Boolean = false
    private val dateFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())

    fun setDarkTheme(dark: Boolean) {
        if (this.isDarkTheme != dark) {
            this.isDarkTheme = dark
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorkoutViewHolder(
        private val binding: ItemWorkoutHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: WorkoutRecord) {
            binding.tvWorkoutDate.text = dateFormat.format(Date(workout.timestamp))
            binding.tvWorkoutDistance.text = String.format(Locale.US, "%.2f km", workout.distanceKm)
            binding.tvWorkoutLaps.text = String.format(Locale.US, "• %d laps", workout.laps)

            // Duration format
            val minutes = workout.durationSeconds / 60
            val seconds = workout.durationSeconds % 60
            binding.tvWorkoutDuration.text = if (minutes >= 60) {
                val hours = minutes / 60
                val remMinutes = minutes % 60
                String.format(Locale.US, "%dh %dm", hours, remMinutes)
            } else {
                String.format(Locale.US, "%dm %02ds", minutes, seconds)
            }

            // Pace format
            if (workout.distanceKm > 0.001) {
                val totalMinutes = workout.durationSeconds / 60.0
                val paceMinPerKm = totalMinutes / workout.distanceKm
                val paceMins = paceMinPerKm.toInt()
                val paceSecs = ((paceMinPerKm - paceMins) * 60).toInt()
                binding.tvWorkoutPace.text = String.format(Locale.US, "%d'%02d\"", paceMins, paceSecs)
            } else {
                binding.tvWorkoutPace.text = "--'--\""
            }

            // Calories format
            binding.tvWorkoutCalories.text = String.format(Locale.US, "%.0f kcal", workout.caloriesBurned)

            binding.btnDeleteWorkout.setOnClickListener {
                onDeleteClick(workout)
            }

            applyThemeStyling()
        }

        private fun applyThemeStyling() {
            if (isDarkTheme) {
                binding.cardWorkoutRoot.setCardBackgroundColor(0xFF131B2E.toInt())
                binding.cardWorkoutRoot.strokeColor = 0xFF223354.toInt()
                binding.tvWorkoutDate.setTextColor(0xFF94A3B8.toInt())
                binding.tvWorkoutDistance.setTextColor(0xFF00E5FF.toInt())
                binding.tvWorkoutLaps.setTextColor(0xFFE2E8F0.toInt())

                binding.badgeDuration.setBackgroundResource(R.drawable.stat_card_bg)
                binding.badgePace.setBackgroundResource(R.drawable.stat_card_bg)
                binding.badgeCalories.setBackgroundResource(R.drawable.stat_card_bg)

                binding.tvLabelDuration.setTextColor(0xFF64748B.toInt())
                binding.tvLabelPace.setTextColor(0xFF64748B.toInt())
                binding.tvLabelCalories.setTextColor(0xFF64748B.toInt())

                binding.tvWorkoutDuration.setTextColor(0xFFFFFFFF.toInt())
                binding.tvWorkoutPace.setTextColor(0xFF00E5FF.toInt())
                binding.tvWorkoutCalories.setTextColor(0xFFFB923C.toInt())
            } else {
                binding.cardWorkoutRoot.setCardBackgroundColor(0xFFFFFFFF.toInt())
                binding.cardWorkoutRoot.strokeColor = 0xFFE2E8F0.toInt()
                binding.tvWorkoutDate.setTextColor(0xFF64748B.toInt())
                binding.tvWorkoutDistance.setTextColor(0xFF0284C7.toInt())
                binding.tvWorkoutLaps.setTextColor(0xFF1E293B.toInt())

                binding.badgeDuration.setBackgroundResource(R.drawable.stat_card_bg_light)
                binding.badgePace.setBackgroundResource(R.drawable.stat_card_bg_light)
                binding.badgeCalories.setBackgroundResource(R.drawable.stat_card_bg_light)

                binding.tvLabelDuration.setTextColor(0xFF94A3B8.toInt())
                binding.tvLabelPace.setTextColor(0xFF94A3B8.toInt())
                binding.tvLabelCalories.setTextColor(0xFF94A3B8.toInt())

                binding.tvWorkoutDuration.setTextColor(0xFF0F172A.toInt())
                binding.tvWorkoutPace.setTextColor(0xFF0284C7.toInt())
                binding.tvWorkoutCalories.setTextColor(0xFFEA580C.toInt())
            }
        }
    }

    private object WorkoutDiffCallback : DiffUtil.ItemCallback<WorkoutRecord>() {
        override fun areItemsTheSame(oldItem: WorkoutRecord, newItem: WorkoutRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkoutRecord, newItem: WorkoutRecord): Boolean {
            return oldItem == newItem
        }
    }
}
