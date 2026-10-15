package com.example.mklive.ar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.mklive.ar.model.ArElement
import com.example.mklive.ar.model.ArElementType
import com.example.mklive.ar.model.BananaElement
import com.example.mklive.ar.model.CoinElement
import com.example.mklive.ar.model.DashPadElement
import com.example.mklive.ar.model.GateElement
import com.example.mklive.ar.model.ItemBoxElement
import com.example.mklive.ar.model.ProjectileElement
import com.example.mklive.ar.tracker.DeadReckoningTracker
import com.example.mklive.audio.SoundManager
import com.example.mklive.combat.CombatManager
import com.example.mklive.combat.ParticleSystem
import com.example.mklive.combat.WeaponType
import com.example.mklive.game.TrackElementData
import com.example.mklive.game.TrackManager
import com.example.mklive.game.TrackPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin

/**
 * Master Augmented Reality & Combat Engine.
 * Coordinates real-time tracking, 3D element rendering, collision detection,
 * combat projectiles, and virtual race track laps.
 */
class ArEngine(
    val soundManager: SoundManager,
    private val scope: CoroutineScope,
    context: Context
) {
    val tracker = DeadReckoningTracker()
    val particleSystem = ParticleSystem()
    val combatManager = CombatManager(soundManager, particleSystem)

    private val elements = CopyOnWriteArrayList<ArElement>()

    // Game & Race Stats
    private val _coinsCollected = MutableStateFlow(0)
    val coinsCollected: StateFlow<Int> = _coinsCollected.asStateFlow()

    private val _currentLap = MutableStateFlow(1)
    val currentLap: StateFlow<Int> = _currentLap.asStateFlow()

    private val _lapTimeSec = MutableStateFlow(0f)
    val lapTimeSec: StateFlow<Int> = MutableStateFlow(0).asStateFlow()

    private val _bestLapTimeSec = MutableStateFlow<Float?>(null)
    val bestLapTimeSec: StateFlow<Float?> = _bestLapTimeSec.asStateFlow()

    private val _isBoostActive = MutableStateFlow(false)
    val isBoostActive: StateFlow<Boolean> = _isBoostActive.asStateFlow()

    private val _isSpinningOut = MutableStateFlow(false)
    val isSpinningOut: StateFlow<Boolean> = _isSpinningOut.asStateFlow()

    var isArEnabled: Boolean = true

    private var boostTimer = 0f
    private var spinTimer = 0f
    private var currentLapTimer = 0f
    private var activeGateNumber = 1

    var onLapCompleted: ((lap: Int, lapTimeSec: Float) -> Unit)? = null
    var onRaceFinished: ((totalTimeSec: Float, lapTimes: List<Float>) -> Unit)? = null
    var onHazardHit: (() -> Unit)? = null

    private val completedLapTimes = mutableListOf<Float>()
    var totalRaceTimer = 0f
    val targetLaps = 3

    init {
        // Initialize 3D bitmap assets and shadows
        ArAssets.init(context)

        // Connect combat callbacks to AR engine
        combatManager.onSpawnProjectile = { isRocket, target, bumperLeft ->
            spawnProjectile(isRocket, target, bumperLeft)
        }
        combatManager.onDropBanana = {
            dropBananaBehindKart()
        }
        combatManager.onTriggerBoost = {
            triggerBoost(2.0f)
        }
        combatManager.onDetonateBobomb = {
            detonateBobomb()
        }

        // Initialize with standard demo race elements in front of kart
        spawnDefaultTrackElements()
    }

    fun loadTrack(preset: TrackPreset) {
        elements.clear()
        tracker.reset(0f, 0f, 0f)
        _coinsCollected.value = 0
        _currentLap.value = 1
        currentLapTimer = 0f
        totalRaceTimer = 0f
        completedLapTimes.clear()
        activeGateNumber = 1

        preset.elements.forEach { elemData ->
            val (wx, wy) = tracker.toWorld(elemData.x, elemData.y)
            val elem: ArElement = when (elemData.type) {
                "COIN" -> CoinElement(UUID.randomUUID().toString(), wx, wy)
                "ITEM_BOX" -> ItemBoxElement(UUID.randomUUID().toString(), wx, wy)
                "DASH_PAD" -> DashPadElement(UUID.randomUUID().toString(), wx, wy)
                "BANANA" -> BananaElement(UUID.randomUUID().toString(), wx, wy)
                "GATE" -> GateElement(
                    UUID.randomUUID().toString(),
                    wx,
                    wy,
                    gateNumber = elemData.gateNumber,
                    isFinishLine = elemData.isFinishLine
                )
                else -> CoinElement(UUID.randomUUID().toString(), wx, wy)
            }
            elements.add(elem)
        }
    }

    fun spawnDefaultTrackElements() {
        val preset = TrackManager.getBuiltinPresets().first()
        loadTrack(preset)
    }

    fun resetTrack() {
        val preset = TrackManager.getBuiltinPresets().first()
        loadTrack(preset)
        soundManager.playCountdownGo()
    }

    fun addVirtualGate() {
        val nextNum = elements.count { it is GateElement } + 1
        val isFinish = nextNum == 1
        val gate = GateElement(
            id = UUID.randomUUID().toString(),
            x = tracker.x,
            y = tracker.y,
            gateNumber = nextNum,
            isFinishLine = isFinish
        )
        elements.add(gate)
        soundManager.playCountdownGo()

        // Auto-spawn 3 coins leading up to next section
        for (i in 1..3) {
            val (cx, cy) = tracker.toWorld(0f, i * 0.8f)
            elements.add(CoinElement(UUID.randomUUID().toString(), cx, cy))
        }
    }

    fun addTrackElement(type: String) {
        val (wx, wy) = Pair(tracker.x, tracker.y)
        val elem: ArElement = when (type.uppercase()) {
            "GATE" -> {
                val nextNum = elements.count { it is GateElement } + 1
                val isFinish = nextNum == 1
                GateElement(UUID.randomUUID().toString(), wx, wy, gateNumber = nextNum, isFinishLine = isFinish)
            }
            "COIN" -> CoinElement(UUID.randomUUID().toString(), wx, wy)
            "ITEM_BOX" -> ItemBoxElement(UUID.randomUUID().toString(), wx, wy)
            "DASH_PAD" -> DashPadElement(UUID.randomUUID().toString(), wx, wy)
            "BANANA" -> BananaElement(UUID.randomUUID().toString(), wx, wy)
            else -> CoinElement(UUID.randomUUID().toString(), wx, wy)
        }
        elements.add(elem)
        soundManager.playCoin()
    }

    fun getPlacedElements(): List<TrackElementData> {
        return elements.map { elem ->
            val rel = tracker.toRelative(elem.x, elem.y)
            when (elem) {
                is GateElement -> TrackElementData("GATE", rel.xRel, rel.yRel, elem.z, elem.gateNumber, elem.isFinishLine)
                is CoinElement -> TrackElementData("COIN", rel.xRel, rel.yRel, elem.z)
                is ItemBoxElement -> TrackElementData("ITEM_BOX", rel.xRel, rel.yRel, elem.z)
                is DashPadElement -> TrackElementData("DASH_PAD", rel.xRel, rel.yRel, elem.z)
                is BananaElement -> TrackElementData("BANANA", rel.xRel, rel.yRel, elem.z)
                else -> TrackElementData("COIN", rel.xRel, rel.yRel, elem.z)
            }
        }
    }

    fun getNextGateAngleDeg(): Float? {
        val nextGate = elements.filterIsInstance<GateElement>().find { it.gateNumber == activeGateNumber && !it.isPassedThisLap }
            ?: elements.filterIsInstance<GateElement>().firstOrNull() ?: return null
        val rel = tracker.toRelative(nextGate.x, nextGate.y)
        val rad = kotlin.math.atan2(rel.xRel, rel.yRel)
        return (rad * 180f / Math.PI.toFloat())
    }

    fun update(
        dt: Float,
        throttle: Int,
        steering: Int,
        brake: Boolean,
        pitchDeg: Float,
        rollDeg: Float,
        yawRateDegS: Float?,
        headingDeg: Float?
    ) {
        if (!isArEnabled) return

        // 1. Update Dead Reckoning Odometry
        tracker.update(
            throttle = throttle,
            steering = steering,
            brake = brake,
            isBoost = _isBoostActive.value,
            gyroYawRateDegS = yawRateDegS,
            headingDeg = headingDeg,
            dtSec = dt
        )

        // 2. Update Timers & Boost States
        currentLapTimer += dt
        totalRaceTimer += dt
        if (boostTimer > 0f) {
            boostTimer -= dt
            _isBoostActive.value = true
        } else {
            _isBoostActive.value = false
        }

        if (spinTimer > 0f) {
            spinTimer -= dt
            _isSpinningOut.value = true
        } else {
            _isSpinningOut.value = false
        }

        // 3. Update Combat & Particle System
        particleSystem.update(dt)

        // 4. Update all elements and handle collisions
        val iterator = elements.iterator()
        var nearestLockTarget: ArElement? = null
        var minTargetDist = 5.0f

        while (iterator.hasNext()) {
            val elem = iterator.next()
            if (!elem.isActive) {
                elements.remove(elem)
                continue
            }

            elem.update(dt)

            // Relative position to kart
            val rel = tracker.toRelative(elem.x, elem.y)

            // Scan for Rocket Lock-On Target (Items, obstacles, gates in front cone)
            if (elem.type == ArElementType.ITEM_BOX || elem.type == ArElementType.BANANA || elem.type == ArElementType.GATE) {
                if (rel.yRel in 0.5f..4.5f && Math.abs(rel.xRel) < 0.65f) {
                    if (rel.distance < minTargetDist) {
                        minTargetDist = rel.distance
                        nearestLockTarget = elem
                    }
                }
            }

            // Check Collision with Kart (Radius overlap or driving over)
            val isColliding = (rel.distance < elem.hitRadius || (rel.yRel in -0.35f..0.60f && kotlin.math.abs(rel.xRel) < elem.hitRadius))
            if (isColliding && !elem.isCollected) {
                handleKartCollision(elem, rel.xRel, rel.yRel)
            }

            // Check Projectile Collisions with other elements
            if (elem is ProjectileElement) {
                checkProjectileImpact(elem)
            }
        }

        combatManager.setLockOnTarget(nearestLockTarget)
    }

    private fun handleKartCollision(elem: ArElement, xRel: Float, yRel: Float) {
        when (elem) {
            is CoinElement -> {
                elem.isActive = false
                elem.isCollected = true
                _coinsCollected.value = minOf(10, _coinsCollected.value + 1)
                soundManager.playCoin()
                // Screen coordinates for particle burst
                val proj = ArGeometry.project(xRel, yRel.coerceAtLeast(0.35f), elem.z, 1280f, 720f)
                particleSystem.emitCoinStars(proj.screenX, proj.screenY)
            }

            is ItemBoxElement -> {
                elem.isActive = false
                elem.isCollected = true
                soundManager.playBoxHit()
                val proj = ArGeometry.project(xRel, yRel.coerceAtLeast(0.35f), elem.z, 1280f, 720f)
                particleSystem.emitBoxShards(proj.screenX, proj.screenY)
                combatManager.startRoulette(scope)
            }

            is DashPadElement -> {
                soundManager.playBoostTurbo()
                triggerBoost(1.5f)
                particleSystem.emitSpeedLines(1280f, 720f)
            }

            is BananaElement -> {
                elem.isActive = false
                soundManager.playSlip()
                spinTimer = 1.0f
                particleSystem.triggerScreenShake(12f)
                onHazardHit?.invoke()
            }

            is GateElement -> {
                if (!elem.isPassedThisLap && elem.gateNumber == activeGateNumber) {
                    elem.isPassedThisLap = true
                    soundManager.playCountdownGo()
                    if (elem.isFinishLine) {
                        // Completed lap!
                        val lapTime = currentLapTimer
                        completedLapTimes.add(lapTime)
                        val best = _bestLapTimeSec.value
                        if (best == null || lapTime < best) {
                            _bestLapTimeSec.value = lapTime
                        }
                        onLapCompleted?.invoke(_currentLap.value, lapTime)
                        currentLapTimer = 0f

                        if (_currentLap.value >= targetLaps) {
                            soundManager.playVictoryJingle()
                            onRaceFinished?.invoke(totalRaceTimer, completedLapTimes.toList())
                        } else {
                            if (_currentLap.value == 2) {
                                soundManager.playFinalLapFanfare()
                            }
                            _currentLap.value++
                            // Reset all gates for next lap
                            elements.filterIsInstance<GateElement>().forEach { it.isPassedThisLap = false }
                            activeGateNumber = 1
                        }
                    } else {
                        activeGateNumber++
                    }
                }
            }
        }
    }

    private fun checkProjectileImpact(proj: ProjectileElement) {
        elements.forEach { target ->
            if (target != proj && target.isActive && !target.isCollected) {
                val dx = target.x - proj.x
                val dy = target.y - proj.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < (target.hitRadius + proj.hitRadius)) {
                    // Direct Hit!
                    proj.isActive = false
                    val rel = tracker.toRelative(target.x, target.y)
                    val p = ArGeometry.project(rel.xRel, rel.yRel, target.z, 1280f, 720f)

                    if (proj.isRocket) {
                        soundManager.playExplosion()
                        particleSystem.emitExplosion(p.screenX, p.screenY)
                        target.isActive = false
                    } else {
                        // Minigun bullet destroys bananas and item boxes
                        if (target is ItemBoxElement) {
                            target.isActive = false
                            soundManager.playBoxHit()
                            particleSystem.emitBoxShards(p.screenX, p.screenY)
                        } else if (target is BananaElement) {
                            target.isActive = false
                            soundManager.playExplosion()
                            particleSystem.emitExplosion(p.screenX, p.screenY)
                        }
                    }
                }
            }
        }
    }

    private fun spawnProjectile(isRocket: Boolean, target: ArElement?, bumperLeft: Boolean) {
        val h = tracker.headingRad
        val lateralOffset = if (bumperLeft) -0.12f else 0.12f
        val spawnX = tracker.x + (cos(h) * lateralOffset) + (sin(h) * 0.25f)
        val spawnY = tracker.y - (sin(h) * lateralOffset) + (cos(h) * 0.25f)

        if (!isRocket) {
            // Muzzle flash at bumper
            val bumperRelX = lateralOffset
            val bumperRelY = 0.25f
            val projScreen = ArGeometry.project(bumperRelX, bumperRelY, 0.04f, 1280f, 720f)
            particleSystem.emitMuzzleFlash(projScreen.screenX, projScreen.screenY)
        }

        val proj = ProjectileElement(
            id = UUID.randomUUID().toString(),
            x = spawnX,
            y = spawnY,
            headingRad = h,
            isRocket = isRocket,
            targetX = target?.x,
            targetY = target?.y
        )
        elements.add(proj)
    }

    private fun dropBananaBehindKart() {
        val h = tracker.headingRad
        val dropX = tracker.x - (sin(h) * 0.35f)
        val dropY = tracker.y - (cos(h) * 0.35f)
        elements.add(BananaElement(UUID.randomUUID().toString(), dropX, dropY))
    }

    fun triggerBoost(durationSec: Float) {
        boostTimer = durationSec
        _isBoostActive.value = true
    }

    private fun detonateBobomb() {
        val h = tracker.headingRad
        val boomX = tracker.x + (sin(h) * 1.5f)
        val boomY = tracker.y + (cos(h) * 1.5f)

        val rel = tracker.toRelative(boomX, boomY)
        val p = ArGeometry.project(rel.xRel, rel.yRel, 0.05f, 1280f, 720f)
        particleSystem.emitExplosion(p.screenX, p.screenY)

        // Clear all obstacles within 1.2m
        elements.forEach { elem ->
            val dx = elem.x - boomX
            val dy = elem.y - boomY
            if (kotlin.math.sqrt(dx * dx + dy * dy) < 1.4f) {
                if (elem is BananaElement || elem is ItemBoxElement) {
                    elem.isActive = false
                }
            }
        }
    }

    /**
     * Renders all 3D AR elements, projectiles, and particle effects.
     */
    fun render(
        canvas: Canvas,
        viewWidth: Float,
        viewHeight: Float,
        pitchDeg: Float,
        rollDeg: Float,
        isLeftEye: Boolean? = null
    ) {
        if (!isArEnabled) return

        canvas.save()

        // Apply Screen Shake if active
        if (particleSystem.screenShakeAmount > 0f) {
            canvas.translate(particleSystem.screenShakeOffsetX, particleSystem.screenShakeOffsetY)
        }

        // Sort elements by distance: render furthest objects first (Painter's Algorithm)
        val sortedList = elements.map { elem ->
            val rel = tracker.toRelative(elem.x, elem.y)
            val proj = ArGeometry.project(
                xRel = rel.xRel,
                yRel = rel.yRel,
                zRel = elem.z,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                pitchDeg = pitchDeg,
                rollDeg = rollDeg,
                isLeftEye = isLeftEye
            )
            Pair(elem, proj)
        }.filter { it.second.isVisible }.sortedByDescending { it.second.distance }

        // Render sorted elements
        sortedList.forEach { (elem, proj) ->
            elem.render(canvas, proj, viewWidth, viewHeight, isLeftEye)
        }

        // Render 2D particles (stars, shards, explosions, smoke, muzzle flash, speed lines)
        particleSystem.render(canvas)

        canvas.restore()
    }
}
