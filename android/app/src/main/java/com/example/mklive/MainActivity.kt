package com.example.mklive

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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
import com.example.mklive.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val viewModel: CockpitViewModel by viewModels()

    // Gamepad & Meta Touch controller state tracking
    @Volatile private var currentAxisX = 0f
    @Volatile private var currentRTrigger = 0f
    @Volatile private var currentLTrigger = 0f
    @Volatile private var padBoost = false
    @Volatile private var padBrake = false
    @Volatile private var activeDeviceName = "Gamepad"
    @Volatile private var isInputActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply selected language (System default / German / English)
        LocaleHelper.applyLanguage(this, LocaleHelper.getLanguage(this))

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

    private fun formatControllerName(rawName: String?): String {
        if (rawName == null) return "Gamepad"
        val lower = rawName.lowercase()
        return when {
            lower.contains("oculus") || lower.contains("touch") || lower.contains("quest") || lower.contains("meta") -> "Quest Touch"
            lower.contains("xbox") -> "Xbox Pad"
            lower.contains("dual") || lower.contains("playstation") || lower.contains("sony") -> "PS Pad"
            lower.contains("switch") || lower.contains("pro controller") -> "Switch Pro"
            else -> rawName.take(12)
        }
    }

    private fun triggerHapticFeedback(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Vibrator::class.java)
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    private fun dispatchGamepadInput() {
        val steer = if (abs(currentAxisX) > 0.05f) (currentAxisX * 127).toInt() else 0

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

            activeDeviceName = formatControllerName(event.device?.name)

            // Steering: support Left Thumbstick (AXIS_X) and Right Thumbstick (AXIS_Z)
            val stickX = event.getAxisValue(MotionEvent.AXIS_X)
            val stickZ = event.getAxisValue(MotionEvent.AXIS_Z)
            currentAxisX = if (abs(stickX) > 0.05f) stickX else stickZ

            // Right Trigger (Index Trigger / Gas): R2
            currentRTrigger = maxOf(
                event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
                event.getAxisValue(MotionEvent.AXIS_GAS)
            )

            // Left Trigger (Index Trigger / Brake / Reverse): L2
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
            activeDeviceName = formatControllerName(event.device?.name)

            when (keyCode) {
                // A or X Button: Full-Throttle BOOST (100%)
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_X -> {
                    if (!padBoost) {
                        padBoost = true
                        triggerHapticFeedback(45)
                        dispatchGamepadInput()
                    }
                    return true
                }

                // Y Button: Quick Toggle VR Mode (SBS / 2D) on Quest & controllers
                KeyEvent.KEYCODE_BUTTON_Y -> {
                    viewModel.toggleVrMode()
                    triggerHapticFeedback(35)
                    return true
                }

                // Grip Buttons (L1 / R1): Brake
                KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L1 -> {
                    if (!padBrake) {
                        padBrake = true
                        triggerHapticFeedback(30)
                        dispatchGamepadInput()
                    }
                    return true
                }

                // Digital Trigger Buttons (Fallback for L2 / R2)
                KeyEvent.KEYCODE_BUTTON_R2 -> {
                    currentRTrigger = 1.0f
                    dispatchGamepadInput()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_L2 -> {
                    currentLTrigger = 1.0f
                    dispatchGamepadInput()
                    return true
                }

                // Thumbstick Clicks (L3 / R3): Reset Heading / Compass
                KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR -> {
                    viewModel.resetHeading()
                    triggerHapticFeedback(25)
                    return true
                }

                // B Button: Intercept back to prevent app closing during gameplay
                KeyEvent.KEYCODE_BUTTON_B -> {
                    if (viewModel.isSettingsOpen.value) {
                        viewModel.closeSettings()
                    } else if (viewModel.isPairingOpen.value) {
                        viewModel.closePairing()
                    } else if (viewModel.isVrMode.value) {
                        viewModel.setVrMode(false)
                    }
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
            } else if (viewModel.isVrMode.value) {
                viewModel.setVrMode(false)
            }
            return true // Never exit app directly during gameplay
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

        if (isGamepad) {
            activeDeviceName = formatControllerName(event.device?.name)

            when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_X -> {
                    padBoost = false
                    dispatchGamepadInput()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L1 -> {
                    padBrake = false
                    dispatchGamepadInput()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R2 -> {
                    currentRTrigger = 0f
                    dispatchGamepadInput()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_L2 -> {
                    currentLTrigger = 0f
                    dispatchGamepadInput()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_Y -> {
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
        } else if (viewModel.isVrMode.value) {
            viewModel.setVrMode(false)
        }
    }
}
