package com.example.model

enum class ClickPhase {
    IDLE,
    CLICKING,
    PAUSED
}

data class SelectedPoint(
    val x: Float = -1f,
    val y: Float = -1f
) {
    val isSet: Boolean get() = x >= 0f && y >= 0f
}

data class AutoClickUiState(
    val selectedPoint: SelectedPoint = SelectedPoint(),
    val clickMinutes: Int = 0,
    val clickSeconds: Int = 10,
    val pauseMinutes: Int = 0,
    val pauseSeconds: Int = 5,
    val clickIntervalMs: Long = 200L, // Interval between tap gestures during click period
    
    val phase: ClickPhase = ClickPhase.IDLE,
    val isRunning: Boolean = false,
    val phaseTimeRemainingSeconds: Int = 0,
    val phaseTotalSeconds: Int = 0,
    val currentCycle: Int = 0,
    val totalClicksInCycle: Long = 0L,
    val totalClicksOverall: Long = 0L,
    
    val isAccessibilityEnabled: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    
    val isSelectingPointOverlayOpen: Boolean = false
)
