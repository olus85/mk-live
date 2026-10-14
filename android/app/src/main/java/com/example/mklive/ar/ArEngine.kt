package com.example.mklive.ar

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
    private val scope: CoroutineScope
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

    init {
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

    fun spawnDefaultTrackElements() {
        elements.clear()

        // 1. Line of 5 golden coins directly ahead
        elements.add(CoinElement(UUID.randomUUID().toString(), 0f, 1.2f))
        elements.add(CoinElement(UUID.randomUUID().toString(), 0f, 2.0f))
        elements.add(CoinElement(UUID.randomUUID().toString(), 0.25f, 2.8f))
        elements.add(CoinElement(UUID.randomUUID().toString(), 0.35f, 3.6f))
        elements.add(CoinElement(UUID.randomUUID().toString(), 0.15f, 4.4f))

        // 2. Holographic ?-Item Box
        elements.add(ItemBoxElement(UUID.randomUUID().toString(), 0f, 5.5f))

        // 3. Turbo Boost Dash Pad
        elements.add(DashPadElement(UUID.randomUUID().toString(), 0f, 7.0f))

        // 4. Finish Gate
        elements.add(GateElement(UUID.randomUUID().toString(), 0f, 9.0f, gateNumber = 1, isFinishLine = true))
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
        val h = tracker.headingRad
        for (i in 1..3) {
            val dist = i * 0.8f
            elements.add(CoinElement(UUID.randomUUID().toString(), tracker.x + sin(h) * dist, tracker.y + cos(h) * dist))
        }
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
        tracker.update(throttle, steering, brake, yawRateDegS, headingDeg, dt)

        // 2. Update Timers & Boost States
        currentLapTimer += dt
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

            // Check Collision with Kart (Radius overlap)
            if (rel.distance < elem.hitRadius && !elem.isCollected) {
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
                val proj = ArGeometry.project(xRel, yRel, elem.z, 1280f, 720f)
                particleSystem.emitCoinStars(proj.screenX, proj.screenY)
            }

            is ItemBoxElement -> {
                elem.isActive = false
                elem.isCollected = true
                soundManager.playBoxHit()
                val proj = ArGeometry.project(xRel, yRel, elem.z, 1280f, 720f)
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
            }

            is GateElement -> {
                if (!elem.isPassedThisLap && elem.gateNumber == activeGateNumber) {
                    elem.isPassedThisLap = true
                    soundManager.playCountdownGo()
                    if (elem.isFinishLine) {
                        // Completed lap!
                        val lapTime = currentLapTimer
                        val best = _bestLapTimeSec.value
                        if (best == null || lapTime < best) {
                            _bestLapTimeSec.value = lapTime
                        }
                        currentLapTimer = 0f
                        _currentLap.value = minOf(3, _currentLap.value + 1)
                        // Reset all gates for next lap
                        elements.filterIsInstance<GateElement>().forEach { it.isPassedThisLap = false }
                        activeGateNumber = 1
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
