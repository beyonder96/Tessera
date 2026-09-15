package com.example

import com.example.viewmodel.FocusMode
import com.example.viewmodel.FocusSoundPlayer
import com.example.viewmodel.PomodoroViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeFocusSoundPlayer : FocusSoundPlayer() {
    var isStarted = false
    override fun start() { isStarted = true }
    override fun stop() { isStarted = false }
}

class PomodoroViewModelTest {

    private lateinit var viewModel: PomodoroViewModel
    private lateinit var fakeSoundPlayer: FakeFocusSoundPlayer

    @Before
    fun setup() {
        fakeSoundPlayer = FakeFocusSoundPlayer()
        viewModel = PomodoroViewModel(fakeSoundPlayer)
    }

    @Test
    fun initialState_isFocusTimerWithDefaults() {
        val state = viewModel.uiState.value
        assertEquals(FocusMode.FOCUS_TIMER, state.selectedMode)
        assertEquals(25, state.focusDuration)
        assertEquals(25, state.currentDuration)
        assertEquals(25 * 60, state.secondsLeft)
        assertFalse(state.isRunning)
        assertEquals("Ocean", state.selectedSoundscape)
        assertEquals("Goal Timer", state.focusModeType)
        assertFalse(state.showSoundscapeDialog)
    }

    @Test
    fun selectMode_switchesModeAndUpdatesSecondsLeft() {
        viewModel.onSelectMode(FocusMode.QUICK_NAP)
        val state = viewModel.uiState.value
        assertEquals(FocusMode.QUICK_NAP, state.selectedMode)
        assertEquals(20, state.currentDuration)
        assertEquals(20 * 60, state.secondsLeft)

        viewModel.onSelectMode(FocusMode.BREATHING)
        val breathingState = viewModel.uiState.value
        assertEquals(FocusMode.BREATHING, breathingState.selectedMode)
        assertEquals(3, breathingState.currentDuration)
        assertEquals(3 * 60, breathingState.secondsLeft)
    }

    @Test
    fun updateDuration_updatesCurrentDurationAndSecondsWhenNotRunning() {
        viewModel.onUpdateDuration(45)
        val state = viewModel.uiState.value
        assertEquals(45, state.focusDuration)
        assertEquals(45, state.currentDuration)
        assertEquals(45 * 60, state.secondsLeft)
    }

    @Test
    fun timerStartAndStop_controlsStateAndSound() {
        viewModel.onStartTimer()
        assertTrue(viewModel.uiState.value.isRunning)
        assertTrue(fakeSoundPlayer.isStarted)

        viewModel.onStopTimer()
        assertFalse(viewModel.uiState.value.isRunning)
        assertFalse(fakeSoundPlayer.isStarted)
        assertEquals(25 * 60, viewModel.uiState.value.secondsLeft)
    }

    @Test
    fun onTick_decrementsSeconds() {
        viewModel.onStartTimer()
        val initialSeconds = viewModel.uiState.value.secondsLeft

        viewModel.onTick()
        assertEquals(initialSeconds - 1, viewModel.uiState.value.secondsLeft)
    }

    @Test
    fun soundscapeSelection_updatesSoundscapeAndClosesDialog() {
        viewModel.onShowSoundscapeDialog(true)
        assertTrue(viewModel.uiState.value.showSoundscapeDialog)

        viewModel.onSelectSoundscape("Rain")
        assertEquals("Rain", viewModel.uiState.value.selectedSoundscape)
        assertFalse(viewModel.uiState.value.showSoundscapeDialog)
    }

    @Test
    fun toggleFocusModeType_switchesBetweenGoalTimerAndStopwatch() {
        assertEquals("Goal Timer", viewModel.uiState.value.focusModeType)
        viewModel.onToggleFocusModeType()
        assertEquals("Stopwatch", viewModel.uiState.value.focusModeType)
        viewModel.onToggleFocusModeType()
        assertEquals("Goal Timer", viewModel.uiState.value.focusModeType)
    }
}
