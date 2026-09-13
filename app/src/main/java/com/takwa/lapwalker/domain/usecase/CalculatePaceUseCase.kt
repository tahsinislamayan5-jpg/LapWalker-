package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.core.math.LapMath

class CalculatePaceUseCase {
    operator fun invoke(elapsedSeconds: Long, laps: Int): Double =
        LapMath.calculateAverageLapTime(elapsedSeconds, laps)
}
