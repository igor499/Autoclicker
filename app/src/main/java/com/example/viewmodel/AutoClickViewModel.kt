package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AutoClickUiState
import com.example.model.ClickPhase
import com.example.model.SelectedPoint
import com.example.service.AutoClickService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoClickViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AutoClickUiState())
    val uiState: StateFlow<AutoClickUiState> = _uiState.asStateFlow()

    private var autoClickJob: Job? = null

    fun checkAccessibilityStatus(context: Context) {
        val isServiceRunning = AutoClickService.isServiceRunning()
        val isServiceEnabled = AutoClickService.isServiceEnabledInSettings(context)
        val active = isServiceRunning || isServiceEnabled
        _uiState.update {
            it.copy(isAccessibilityEnabled = active)
        }
    }

    fun selectPoint(x: Float, y: Float) {
        _uiState.update {
            it.copy(
                selectedPoint = SelectedPoint(x, y),
                isSelectingPointOverlayOpen = false,
                errorMessage = null,
                infoMessage = "Target point set at X: ${x.toInt()}, Y: ${y.toInt()}"
            )
        }
    }

    fun setSelectingOverlayOpen(isOpen: Boolean) {
        _uiState.update {
            it.copy(isSelectingPointOverlayOpen = isOpen)
        }
    }

    fun setClickDuration(minutes: Int, seconds: Int) {
        val min = minutes.coerceIn(0, 99)
        val sec = seconds.coerceIn(0, 59)
        _uiState.update {
            it.copy(
                clickMinutes = min,
                clickSeconds = sec,
                errorMessage = null
            )
        }
    }

    fun setPauseDuration(minutes: Int, seconds: Int) {
        val min = minutes.coerceIn(0, 99)
        val sec = seconds.coerceIn(0, 59)
        _uiState.update {
            it.copy(
                pauseMinutes = min,
                pauseSeconds = sec,
                errorMessage = null
            )
        }
    }

    fun setClickInterval(intervalMs: Long) {
        val ms = intervalMs.coerceIn(50L, 5000L)
        _uiState.update {
            it.copy(clickIntervalMs = ms)
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(errorMessage = null, infoMessage = null)
        }
    }

    fun startAutoClicker(context: Context) {
        // 1. Check point selected
        val point = _uiState.value.selectedPoint
        if (!point.isSet) {
            _uiState.update {
                it.copy(errorMessage = "Please select a target point on screen first by tapping 'Select point'.")
            }
            return
        }

        // 2. Check duration inputs
        val totalClickSec = _uiState.value.clickMinutes * 60 + _uiState.value.clickSeconds
        val totalPauseSec = _uiState.value.pauseMinutes * 60 + _uiState.value.pauseSeconds

        if (totalClickSec <= 0) {
            _uiState.update {
                it.copy(errorMessage = "Clicking duration must be greater than 0 seconds.")
            }
            return
        }

        if (totalPauseSec <= 0) {
            _uiState.update {
                it.copy(errorMessage = "Pause duration must be greater than 0 seconds.")
            }
            return
        }

        // 3. Check accessibility service
        checkAccessibilityStatus(context)
        val isServiceAvailable = AutoClickService.isServiceRunning()
        if (!isServiceAvailable) {
            _uiState.update {
                it.copy(errorMessage = "Accessibility Service is not running. Please enable AutoClicker in System Settings.")
            }
            return
        }

        // Stop any existing job
        autoClickJob?.cancel()

        _uiState.update {
            it.copy(
                isRunning = true,
                errorMessage = null,
                infoMessage = "AutoClicker started successfully!",
                totalClicksOverall = 0L
            )
        }

        autoClickJob = viewModelScope.launch(Dispatchers.Default) {
            var cycleCount = 1

            while (isActive && _uiState.value.isRunning) {
                val currentPoint = _uiState.value.selectedPoint
                val clickInterval = _uiState.value.clickIntervalMs

                // ===== CLICKING PHASE =====
                _uiState.update {
                    it.copy(
                        phase = ClickPhase.CLICKING,
                        phaseTotalSeconds = totalClickSec,
                        phaseTimeRemainingSeconds = totalClickSec,
                        currentCycle = cycleCount,
                        totalClicksInCycle = 0L
                    )
                }

                val clickStartTime = System.currentTimeMillis()
                val clickEndTime = clickStartTime + (totalClickSec * 1000L)
                var lastTapTime = 0L

                while (isActive && _uiState.value.isRunning && System.currentTimeMillis() < clickEndTime) {
                    val now = System.currentTimeMillis()
                    val remainingMs = (clickEndTime - now).coerceAtLeast(0L)
                    val remainingSec = ((remainingMs + 999L) / 1000L).toInt()

                    if (_uiState.value.phaseTimeRemainingSeconds != remainingSec) {
                        _uiState.update { it.copy(phaseTimeRemainingSeconds = remainingSec) }
                    }

                    // Perform tap at interval
                    if (now - lastTapTime >= clickInterval) {
                        lastTapTime = now
                        val service = AutoClickService.instance
                        val success = service?.tapAt(currentPoint.x, currentPoint.y) ?: false
                        if (success) {
                            _uiState.update {
                                it.copy(
                                    totalClicksInCycle = it.totalClicksInCycle + 1,
                                    totalClicksOverall = it.totalClicksOverall + 1
                                )
                            }
                        }
                    }

                    delay(20L)
                }

                if (!isActive || !_uiState.value.isRunning) break

                // ===== PAUSE PHASE =====
                _uiState.update {
                    it.copy(
                        phase = ClickPhase.PAUSED,
                        phaseTotalSeconds = totalPauseSec,
                        phaseTimeRemainingSeconds = totalPauseSec
                    )
                }

                val pauseStartTime = System.currentTimeMillis()
                val pauseEndTime = pauseStartTime + (totalPauseSec * 1000L)

                while (isActive && _uiState.value.isRunning && System.currentTimeMillis() < pauseEndTime) {
                    val now = System.currentTimeMillis()
                    val remainingMs = (pauseEndTime - now).coerceAtLeast(0L)
                    val remainingSec = ((remainingMs + 999L) / 1000L).toInt()

                    if (_uiState.value.phaseTimeRemainingSeconds != remainingSec) {
                        _uiState.update { it.copy(phaseTimeRemainingSeconds = remainingSec) }
                    }

                    delay(100L)
                }

                if (!isActive || !_uiState.value.isRunning) break

                cycleCount++
            }

            _uiState.update {
                it.copy(
                    phase = ClickPhase.IDLE,
                    isRunning = false,
                    phaseTimeRemainingSeconds = 0,
                    phaseTotalSeconds = 0,
                    infoMessage = "AutoClicker stopped."
                )
            }
        }
    }

    fun stopAutoClicker() {
        autoClickJob?.cancel()
        autoClickJob = null
        _uiState.update {
            it.copy(
                phase = ClickPhase.IDLE,
                isRunning = false,
                phaseTimeRemainingSeconds = 0,
                phaseTotalSeconds = 0,
                infoMessage = "AutoClicker stopped by user."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoClickJob?.cancel()
    }
}
