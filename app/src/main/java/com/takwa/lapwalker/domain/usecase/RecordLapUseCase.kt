package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.model.WalkSessionState

class RecordLapUseCase {
    operator fun invoke(currentState: WalkSessionState): WalkSessionState {
        val nextLap = currentState.laps + 1
        return currentState.copy(laps = nextLap)
    }
}
