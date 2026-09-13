package com.takwa.lapwalker.domain.model.calisthenics

data class CalisthenicsStep(
    val id: String,
    val pillar: CalisthenicsPillar,
    val stepNumber: Int,
    val name: String,
    val instructions: String,
    val formCues: List<String>,
    val metricType: ExerciseMetricType,
    val targetSets: Int,
    val targetRepsOrSeconds: Int,
    val restDurationSeconds: Int,
    val unlockBenchmarkText: String,
    val nextStepId: String? = null
)
