package com.example.mklive.ui.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * On-screen touch controller overlay for Mario Kart Live.
 *
 * Rules:
 * 1. Automatically hidden/disabled when an external gamepad is active.
 * 2. When the user stops touching, driving immediately stops (throttle=0, steer=0),
 *    and on-screen visual controls remain visible for ~1.8 seconds before smoothly fading out.
 * 3. Left half: Floating virtual joystick for steering (-128 to +127).
 * 4. Right half: Floating vertical throttle stick (up = forward up to +127, down = reverse -90)
 *    and quick Action buttons (A: Boost, R1: Brake/Light).
 */
@Composable
fun VirtualTouchOverlay(
    isGamepadActive: Boolean,
    onDriveCommand: (throttle: Int, steering: Int, brake: Boolean, source: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGamepadActive) {
        // External controller is actively used: hide and disable touch controls completely
        return
    }

    val coroutineScope = rememberCoroutineScope()

    var leftTouchOrigin by remember { mutableStateOf<Offset?>(null) }
    var leftTouchCurrent by remember { mutableStateOf<Offset?>(null) }
    var isLeftTouching by remember { mutableStateOf(false) }
    var showLeftControls by remember { mutableStateOf(false) }
    var leftFadeJob by remember { mutableStateOf<Job?>(null) }

    var rightTouchOrigin by remember { mutableStateOf<Offset?>(null) }
    var rightTouchCurrent by remember { mutableStateOf<Offset?>(null) }
    var isRightTouching by remember { mutableStateOf(false) }
    var showRightControls by remember { mutableStateOf(false) }
    var rightFadeJob by remember { mutableStateOf<Job?>(null) }

    var isBoostActive by remember { mutableStateOf(false) }
    var isBrakeActive by remember { mutableStateOf(false) }

    val maxStickRadius = 110f

    // Calculate current values
    var steeringValue by remember { mutableFloatStateOf(0f) }
    var throttleValue by remember { mutableFloatStateOf(0f) }

    fun updateCommand() {
        val t = if (isBoostActive) 127 else throttleValue.toInt()
        val s = steeringValue.toInt()
        val b = isBrakeActive
        onDriveCommand(t, s, b, "touch")
    }

    val leftAlpha by animateFloatAsState(
        targetValue = if (showLeftControls && leftTouchOrigin != null) (if (isLeftTouching) 1.0f else 0.45f) else 0f,
        animationSpec = tween(if (isLeftTouching) 100 else 600),
        label = "leftAlpha"
    )

    val rightAlpha by animateFloatAsState(
        targetValue = if (showRightControls && rightTouchOrigin != null) (if (isRightTouching) 1.0f else 0.45f) else 0f,
        animationSpec = tween(if (isRightTouching) 100 else 600),
        label = "rightAlpha"
    )

    val actionButtonsAlpha by animateFloatAsState(
        targetValue = if (showLeftControls || showRightControls) 1.0f else 0f,
        animationSpec = tween(if (isLeftTouching || isRightTouching) 100 else 600),
        label = "actionAlpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT ZONE: Steering
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue

                                if (change.pressed) {
                                    leftFadeJob?.cancel()
                                    isLeftTouching = true
                                    showLeftControls = true

                                    if (leftTouchOrigin == null) {
                                        leftTouchOrigin = change.position
                                        leftTouchCurrent = change.position
                                    } else {
                                        leftTouchCurrent = change.position
                                        val origin = leftTouchOrigin!!
                                        val dx = change.position.x - origin.x
                                        val dy = change.position.y - origin.y
                                        val dist = sqrt(dx * dx + dy * dy)
                                        val clampedDist = min(dist, maxStickRadius)
                                        val angle = atan2(dy, dx)

                                        val clampedX = cos(angle) * clampedDist
                                        val normalizedX = (clampedX / maxStickRadius).coerceIn(-1f, 1f)
                                        val s = if (kotlin.math.abs(normalizedX) < 0.05f) 0f else normalizedX
                                        steeringValue = s * 127f
                                        updateCommand()
                                    }
                                } else {
                                    // Finger lifted: immediately reset steering to neutral for safety
                                    isLeftTouching = false
                                    steeringValue = 0f
                                    updateCommand()

                                    // Retain visual controls for ~1.8 seconds before smooth fade-out
                                    leftFadeJob?.cancel()
                                    leftFadeJob = coroutineScope.launch {
                                        delay(1800)
                                        showLeftControls = false
                                        leftTouchOrigin = null
                                        leftTouchCurrent = null
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Render dynamic left thumbstick
                if (leftAlpha > 0f) {
                    leftTouchOrigin?.let { origin ->
                        val current = if (isLeftTouching) (leftTouchCurrent ?: origin) else origin
                        val dx = current.x - origin.x
                        val dy = current.y - origin.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val clampedDist = min(dist, maxStickRadius)
                        val angle = atan2(dy, dx)
                        val thumbX = origin.x + cos(angle) * clampedDist
                        val thumbY = origin.y + sin(angle) * clampedDist

                        val baseA = 0.35f * leftAlpha
                        val borderA = 0.85f * leftAlpha
                        val thumbA = 0.95f * leftAlpha

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Base outer circle
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = baseA),
                                radius = maxStickRadius,
                                center = origin
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = borderA),
                                radius = maxStickRadius,
                                center = origin,
                                style = Stroke(width = 3f)
                            )
                            // Moving Thumb
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = thumbA),
                                radius = 45f,
                                center = Offset(thumbX, thumbY)
                            )
                        }
                    }
                }
            }

            // RIGHT ZONE: Throttle & Reverse
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue

                                if (change.pressed) {
                                    rightFadeJob?.cancel()
                                    isRightTouching = true
                                    showRightControls = true

                                    if (rightTouchOrigin == null) {
                                        rightTouchOrigin = change.position
                                        rightTouchCurrent = change.position
                                    } else {
                                        rightTouchCurrent = change.position
                                        val origin = rightTouchOrigin!!
                                        val dy = origin.y - change.position.y // Up is positive gas
                                        val clampedY = (dy / maxStickRadius).coerceIn(-1f, 1f)

                                        throttleValue = if (clampedY > 0) {
                                            clampedY * 90f // Gas up to 70% (90); Boost button provides 100% (127)
                                        } else {
                                            clampedY * 90f // Reverse down to -90 (70%)
                                        }
                                        updateCommand()
                                    }
                                } else {
                                    // Finger lifted: immediately cut throttle to zero for safety
                                    isRightTouching = false
                                    throttleValue = 0f
                                    updateCommand()

                                    // Retain visual controls for ~1.8 seconds before smooth fade-out
                                    rightFadeJob?.cancel()
                                    rightFadeJob = coroutineScope.launch {
                                        delay(1800)
                                        showRightControls = false
                                        rightTouchOrigin = null
                                        rightTouchCurrent = null
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Render dynamic right throttle stick
                if (rightAlpha > 0f) {
                    rightTouchOrigin?.let { origin ->
                        val current = if (isRightTouching) (rightTouchCurrent ?: origin) else origin
                        val dy = (origin.y - current.y).coerceIn(-maxStickRadius, maxStickRadius)
                        val thumbY = origin.y - dy

                        val trackA = 0.50f * rightAlpha
                        val centerA = 0.35f * rightAlpha
                        val thumbA = 0.95f * rightAlpha

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Vertical track
                            drawLine(
                                color = Color(0xFFFF9100).copy(alpha = trackA),
                                start = Offset(origin.x, origin.y - maxStickRadius),
                                end = Offset(origin.x, origin.y + maxStickRadius),
                                strokeWidth = 6f
                            )
                            drawCircle(
                                color = Color(0xFFFF9100).copy(alpha = centerA),
                                radius = 35f,
                                center = origin
                            )
                            // Moving Thumb
                            drawCircle(
                                color = Color(0xFFFF9100).copy(alpha = thumbA),
                                radius = 45f,
                                center = Offset(origin.x, thumbY)
                            )
                        }
                    }
                }
            }
        }

        // Action buttons: Boost & Brake (visible during touch or grace delay)
        if (actionButtonsAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 32.dp)
                    .alpha(actionButtonsAlpha)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Brake button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isBrakeActive) Color(0xFFFF1744) else Color(0x88D50000))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isBrakeActive = true
                                        updateCommand()
                                        tryAwaitRelease()
                                        isBrakeActive = false
                                        updateCommand()
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = "BRAKE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Boost / Gas button (A)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isBoostActive) Color(0xFF00E676) else Color(0x8800B0FF))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isBoostActive = true
                                        updateCommand()
                                        tryAwaitRelease()
                                        isBoostActive = false
                                        updateCommand()
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = "BOOST",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
