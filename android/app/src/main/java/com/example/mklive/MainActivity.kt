package com.example.mklive

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.mklive.theme.MKLiveTheme
import com.example.mklive.ui.main.CockpitScreen
import com.example.mklive.ui.main.CockpitViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: CockpitViewModel by viewModels()

    // Gamepad state tracking
    @Volatile private var currentAxisX = 0f
    @Volatile private var currentRTrigger = 0f
    @Volatile private var currentLTrigger = 0f
    @Volatile private var padBoost = false
    @Volatile private var padBrake = false
    @Volatile private var activeDeviceName = "Gamepad"
    @Volatile private var isInputActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen awake while driving
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enable immersive sticky fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Continuous 30 Hz ticker: ensures holding R2 or stick sends steady stream to prevent server deadman cutoff
        lifecycleScope.launch {
            while (isActive) {
                delay(33) // ~30 Hz
                if (isInputActive) {
                    dispatchGamepadInput()
                }
            }
        }

        setContent {
            MKLiveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    CockpitScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun dispatchGamepadInput() {
        val steer = if (kotlin.math.abs(currentAxisX) > 0.05f) (currentAxisX * 127).toInt() else 0

        // R2 Gas: standard up to 70% (value 90). Button A / X Boost delivers 100% (value 127).
        val forward = if (padBoost) 127 else if (currentRTrigger > 0.05f) (currentRTrigger * 90).toInt() else 0
        val reverse = if (currentLTrigger > 0.05f) (currentLTrigger * 90).toInt() else 0
        val throttle = forward - reverse

        val hasInput = throttle != 0 || steer != 0 || padBrake || padBoost

        if (hasInput || isInputActive) {
            viewModel.handleGamepadInput(throttle, steer, padBrake, activeDeviceName)
            isInputActive = hasInput
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {

            activeDeviceName = event.device?.name?.take(20) ?: "Gamepad"

            // Left Stick X axis: Steering (-1.0 to +1.0)
            currentAxisX = event.getAxisValue(MotionEvent.AXIS_X)

            // Right Trigger (R2): Gas
            currentRTrigger = maxOf(
                event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
                event.getAxisValue(MotionEvent.AXIS_GAS)
            )

            // Left Trigger (L2): Reverse
            currentLTrigger = maxOf(
                event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
                event.getAxisValue(MotionEvent.AXIS_BRAKE)
            )

            dispatchGamepadInput()
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

        if (isGamepad) {
            activeDeviceName = event.device?.name?.take(20) ?: "Gamepad"

            when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_X -> {
                    padBoost = true
                    dispatchGamepadInput()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_B -> {
                    // Prevent Button B on controller from closing the application!
                    if (viewModel.isSettingsOpen.value) {
                        viewModel.closeSettings()
                    } else if (viewModel.isPairingOpen.value) {
                        viewModel.closePairing()
                    }
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R1 -> {
                    padBrake = true
                    dispatchGamepadInput()
                    return true
                }
            }
        }

        // Intercept BACK key from hardware / navigation gesture
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (viewModel.isSettingsOpen.value) {
                viewModel.closeSettings()
            } else if (viewModel.isPairingOpen.value) {
                viewModel.closePairing()
            }
            return true // Never exit app directly during gameplay
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

        if (isGamepad) {
            activeDeviceName = event.device?.name?.take(20) ?: "Gamepad"

            when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_X -> {
                    padBoost = false
                    dispatchGamepadInput()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_B -> {
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R1 -> {
                    padBrake = false
                    dispatchGamepadInput()
                    return true
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.isSettingsOpen.value) {
            viewModel.closeSettings()
        } else if (viewModel.isPairingOpen.value) {
            viewModel.closePairing()
        }
    }
}
