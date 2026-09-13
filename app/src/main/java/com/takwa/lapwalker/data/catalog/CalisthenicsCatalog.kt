package com.takwa.lapwalker.data.catalog

import com.takwa.lapwalker.domain.model.calisthenics.CalisthenicsPillar
import com.takwa.lapwalker.domain.model.calisthenics.CalisthenicsStep
import com.takwa.lapwalker.domain.model.calisthenics.ExerciseMetricType

object CalisthenicsCatalog {

    val steps = listOf(
        // PILLAR 1: PUSH
        CalisthenicsStep(
            id = "push_1",
            pillar = CalisthenicsPillar.PUSH,
            stepNumber = 1,
            name = "Wall Push-ups",
            instructions = "Stand 2 to 3 feet from wall. Palms flat at chest height. Keep core rigid. Lower face to wall, pause, press back.",
            formCues = listOf("Elbows at 45 degrees", "Straight body line", "2s down, 1s up"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 3,
            targetRepsOrSeconds = 15,
            restDurationSeconds = 60,
            unlockBenchmarkText = "Complete 3 sets of 15 reps with 60s rest intervals",
            nextStepId = "push_2"
        ),
        CalisthenicsStep(
            id = "push_2",
            pillar = CalisthenicsPillar.PUSH,
            stepNumber = 2,
            name = "High Incline Push-ups",
            instructions = "Hands on waist-high workbench, car hood, or table (~45 deg). Lower chest to tap edge, press to full lockout.",
            formCues = listOf("Do not sag hips", "Drive through palms", "No bouncing"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 3,
            targetRepsOrSeconds = 12,
            restDurationSeconds = 60,
            unlockBenchmarkText = "Complete 3 sets of 12 reps with 60s rest",
            nextStepId = "push_3"
        ),
        CalisthenicsStep(
            id = "push_3",
            pillar = CalisthenicsPillar.PUSH,
            stepNumber = 3,
            name = "Low Incline Push-ups",
            instructions = "Hands on knee-high sturdy chair or bench (~30 deg). Heavy chest load.",
            formCues = listOf("Core braced", "Chest touches chair edge", "2s descent"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 2,
            targetRepsOrSeconds = 10,
            restDurationSeconds = 60,
            unlockBenchmarkText = "Complete 2 sets of 10 clean reps with 60s rest",
            nextStepId = "push_4"
        ),
        CalisthenicsStep(
            id = "push_4",
            pillar = CalisthenicsPillar.PUSH,
            stepNumber = 4,
            name = "Floor Eccentric Negatives",
            instructions = "Start at top of full floor push-up on toes. Take 4 to 5 full seconds to slowly lower chest to floor. Reset on knees.",
            formCues = listOf("No dropping at bottom", "Count: 1-one-thousand, 2-one-thousand...", "Tension to floor"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 1,
            targetRepsOrSeconds = 5,
            restDurationSeconds = 90,
            unlockBenchmarkText = "Complete 5 continuous 5-second slow negatives in a row",
            nextStepId = "push_5"
        ),
        CalisthenicsStep(
            id = "push_5",
            pillar = CalisthenicsPillar.PUSH,
            stepNumber = 5,
            name = "The Strict Floor Push-Up",
            instructions = "Full floor push-up on toes and palms. Chest touches ground, locked elbows at top. Strict form.",
            formCues = listOf("Straight back", "Full range", "Glutes locked"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 1,
            targetRepsOrSeconds = 1,
            restDurationSeconds = 0,
            unlockBenchmarkText = "Complete 1 strict clean floor push-up",
            nextStepId = null
        ),

        // PILLAR 2: PULL
        CalisthenicsStep(
            id = "pull_1",
            pillar = CalisthenicsPillar.PULL,
            stepNumber = 1,
            name = "Doorframe Rows",
            instructions = "Stand in garage doorway, grip trim at chest height, toes near base. Lean straight back. Pull chest into frame.",
            formCues = listOf("Squeeze shoulder blades", "Straight body plank", "Don't yank with arms only"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 3,
            targetRepsOrSeconds = 15,
            restDurationSeconds = 60,
            unlockBenchmarkText = "Complete 3 sets of 15 clean reps",
            nextStepId = "pull_2"
        ),
        CalisthenicsStep(
            id = "pull_2",
            pillar = CalisthenicsPillar.PULL,
            stepNumber = 2,
            name = "Deep Towel Rows",
            instructions = "Knot towel over closed door. Hold ends, lean back at steep 45 deg. Pull chest to hands.",
            formCues = listOf("Lead with elbows", "Wrists straight", "Proud chest"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 3,
            targetRepsOrSeconds = 12,
            restDurationSeconds = 60,
            unlockBenchmarkText = "Complete 3 sets of 12 reps",
            nextStepId = "pull_3"
        ),
        CalisthenicsStep(
            id = "pull_3",
            pillar = CalisthenicsPillar.PULL,
            stepNumber = 3,
            name = "Inverted Table Rows",
            instructions = "Lie under sturdy workbench/table. Overhand grip on edge. Pull chest up to touch tabletop.",
            formCues = listOf("Heels dug in", "Full body tension", "Pause 1s at top"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 3,
            targetRepsOrSeconds = 8,
            restDurationSeconds = 90,
            unlockBenchmarkText = "Complete 3 sets of 8 strict reps",
            nextStepId = null
        ),

        // PILLAR 3: LEGS
        CalisthenicsStep(
            id = "legs_1",
            pillar = CalisthenicsPillar.LEGS,
            stepNumber = 1,
            name = "Chair / Box Squats",
            instructions = "Stand in front of chair. Sit back slowly until butt taps seat (no flopping). Drive up through heels.",
            formCues = listOf("Knees track over toes", "Chest up", "Weight in heels"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 3,
            targetRepsOrSeconds = 15,
            restDurationSeconds = 60,
            unlockBenchmarkText = "Complete 3 sets of 15 clean reps",
            nextStepId = "legs_2"
        ),
        CalisthenicsStep(
            id = "legs_2",
            pillar = CalisthenicsPillar.LEGS,
            stepNumber = 2,
            name = "Full Air Squats",
            instructions = "Bodyweight squat without chair. Lower until thighs parallel to ground. Stand tall and squeeze glutes.",
            formCues = listOf("Heels glued to floor", "Deep breath on way down", "Explode up"),
            metricType = ExerciseMetricType.REPS,
            targetSets = 3,
            targetRepsOrSeconds = 20,
            restDurationSeconds = 60,
            unlockBenchmarkText = "Complete 3 sets of 20 continuous reps",
            nextStepId = "legs_3"
        ),
        CalisthenicsStep(
            id = "legs_3",
            pillar = CalisthenicsPillar.LEGS,
            stepNumber = 3,
            name = "The Garage Wall-Sit",
            instructions = "Back flat against wall. Slide down until thighs parallel to floor, knees at 90 deg. Hold motionless.",
            formCues = listOf("Hands off thighs", "Breathe steadily", "Back pressed hard"),
            metricType = ExerciseMetricType.TIME_HOLD,
            targetSets = 1,
            targetRepsOrSeconds = 45,
            restDurationSeconds = 0,
            unlockBenchmarkText = "Hold continuously for 45 seconds",
            nextStepId = null
        ),

        // PILLAR 4: CORE
        CalisthenicsStep(
            id = "core_1",
            pillar = CalisthenicsPillar.CORE,
            stepNumber = 1,
            name = "Incline Plank",
            instructions = "Forearms on waist-high table or workbench. Rigid plank posture. Squeeze abs and glutes.",
            formCues = listOf("Don't sag hips", "Pull belly button to spine", "Breathe through nose"),
            metricType = ExerciseMetricType.TIME_HOLD,
            targetSets = 1,
            targetRepsOrSeconds = 45,
            restDurationSeconds = 0,
            unlockBenchmarkText = "Hold continuously for 45 seconds",
            nextStepId = "core_2"
        ),
        CalisthenicsStep(
            id = "core_2",
            pillar = CalisthenicsPillar.CORE,
            stepNumber = 2,
            name = "Knee Floor Plank",
            instructions = "On floor on forearms and knees. Straight line from knees to head. Squeeze abs tight.",
            formCues = listOf("Elbows under shoulders", "No arching lower back"),
            metricType = ExerciseMetricType.TIME_HOLD,
            targetSets = 1,
            targetRepsOrSeconds = 45,
            restDurationSeconds = 0,
            unlockBenchmarkText = "Hold continuously for 45 seconds",
            nextStepId = "core_3"
        ),
        CalisthenicsStep(
            id = "core_3",
            pillar = CalisthenicsPillar.CORE,
            stepNumber = 3,
            name = "The Full Floor Plank",
            instructions = "Standard forearm plank on toes and elbows. Full body tension like a steel bridge.",
            formCues = listOf("Glutes clenched", "Forearms parallel", "Never hold breath"),
            metricType = ExerciseMetricType.TIME_HOLD,
            targetSets = 1,
            targetRepsOrSeconds = 60,
            restDurationSeconds = 0,
            unlockBenchmarkText = "Hold continuously for 60 seconds",
            nextStepId = null
        )
    )

    fun getStep(id: String): CalisthenicsStep? {
        return steps.find { it.id == id }
    }

    fun getStepsForPillar(pillar: CalisthenicsPillar): List<CalisthenicsStep> {
        return steps.filter { it.pillar == pillar }
    }
}
