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

    var isDynamicRoomArcade: Boolean = true
    var isDriveAndPaintMode: Boolean = false
    val trackBreadcrumbs = CopyOnWriteArrayList<Pair<Float, Float>>()
    private var lastRecordedPoint: Pair<Float, Float>? = null
    private var totalDrivenDist: Float = 0f
    private var autoPlacedGateCount: Int = 1

    private val trackRibbonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        setShadowLayer(10f, 0f, 0f, Color.parseColor("#00B0FF"))
    }

    private val trackRibbonCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80D8FF")
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
    }

    private val trackRibbonFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 0, 229, 255)
        style = Paint.Style.FILL
    }

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

    fun getTrackBreadcrumbsRelative(): List<Pair<Float, Float>> {
        return trackBreadcrumbs.map { pt ->
            val rel = tracker.toRelative(pt.first, pt.second)
            Pair(rel.xRel, rel.yRel)
        }
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
        isDynamicRoomArcade = (preset.id == "room_dynamic_arcade")
        isDriveAndPaintMode = (preset.id == "track_builder_circuit" || preset.id.startsWith("custom_"))
        trackBreadcrumbs.clear()
        lastRecordedPoint = null
        totalDrivenDist = 0f
        autoPlacedGateCount = 1

        if (preset.id == "track_builder_circuit") {
            // Place start/finish gate 1.2m ahead of kart so player drives through it!
            val (g1x, g1y) = tracker.toWorld(0.0f, 1.2f)
            elements.add(GateElement(UUID.randomUUID().toString(), g1x, g1y, gateNumber = 1, isFinishLine = true))
            trackBreadcrumbs.add(Pair(tracker.x, tracker.y))
            trackBreadcrumbs.add(Pair(g1x, g1y))
            lastRecordedPoint = Pair(g1x, g1y)
        } else if (preset.breadcrumbs.isNotEmpty()) {
            // Restore saved custom track ribbon
            preset.breadcrumbs.forEach { (bx, by) ->
                val (wx, wy) = tracker.toWorld(bx, by)
                trackBreadcrumbs.add(Pair(wx, wy))
            }
        } else if (isDynamicRoomArcade) {
            // Procedural glowing cyber corridor leading through Gate 1
            for (step in 0..15) {
                val yAhead = step * 0.35f
                val (wx, wy) = tracker.toWorld(0f, yAhead)
                trackBreadcrumbs.add(Pair(wx, wy))
            }
        }

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

    private fun spawnNextDynamicGate(gateNum: Int) {
        // Distance ahead for next checkpoint: 3.2m directly in the forward driving path
        val (gx, gy) = tracker.toWorld(0.0f, 3.2f)
        val isFinish = (gateNum == 4)
        val gate = GateElement(
            id = UUID.randomUUID().toString(),
            x = gx,
            y = gy,
            gateNumber = gateNum,
            isFinishLine = isFinish
        )
        elements.add(gate)

        // Extend cyber ribbon forward to the gate
        for (step in 1..8) {
            val (wx, wy) = tracker.toWorld(0.0f, step * 0.40f)
            trackBreadcrumbs.add(Pair(wx, wy))
        }

        // Spawn interactive arcade item / coin cluster along the corridor
        val rand = kotlin.random.Random.nextInt(3)
        val (ix, iy) = tracker.toWorld(0.0f, 1.8f)
        when (rand) {
            0 -> elements.add(ItemBoxElement(UUID.randomUUID().toString(), ix, iy))
            1 -> elements.add(DashPadElement(UUID.randomUUID().toString(), ix, iy))
            else -> {
                elements.add(CoinElement(UUID.randomUUID().toString(), ix, iy))
                val (c2x, c2y) = tracker.toWorld(0.0f, 2.4f)
                elements.add(CoinElement(UUID.randomUUID().toString(), c2x, c2y))
            }
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

        // 2. Drive & Paint: Record live floor trail (Spur)
        if (isDriveAndPaintMode && kotlin.math.abs(tracker.currentSpeedMps) > 0.05f) {
            val cur = Pair(tracker.x, tracker.y)
            val last = lastRecordedPoint
            val d = if (last != null) {
                val dx = cur.first - last.first
                val dy = cur.second - last.second
                kotlin.math.sqrt(dx * dx + dy * dy)
            } else 1.0f

            if (d >= 0.18f) {
                trackBreadcrumbs.add(cur)
                lastRecordedPoint = cur
                totalDrivenDist += d

                // Auto-place checkpoints as the user drives their circuit
                if (autoPlacedGateCount == 1 && totalDrivenDist >= 3.0f) {
                    autoPlacedGateCount = 2
                    elements.add(GateElement(UUID.randomUUID().toString(), cur.first, cur.second, gateNumber = 2, isFinishLine = false))
                    soundManager.playCountdownGo()
                } else if (autoPlacedGateCount == 2 && totalDrivenDist >= 6.0f) {
                    autoPlacedGateCount = 3
                    elements.add(GateElement(UUID.randomUUID().toString(), cur.first, cur.second, gateNumber = 3, isFinishLine = false))
                    soundManager.playCountdownGo()
                } else if (autoPlacedGateCount == 3 && totalDrivenDist >= 9.0f) {
                    autoPlacedGateCount = 4
                    elements.add(GateElement(UUID.randomUUID().toString(), cur.first, cur.second, gateNumber = 4, isFinishLine = false))
                    soundManager.playCountdownGo()
                } else if (autoPlacedGateCount >= 4 && totalDrivenDist >= 11.0f) {
                    // Check if returning close to Gate 1 (start gate)
                    val gate1 = elements.find { it is GateElement && it.gateNumber == 1 }
                    if (gate1 != null) {
                        val dx = cur.first - gate1.x
                        val dy = cur.second - gate1.y
                        val distToStart = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (distToStart < 1.4f) {
                            // Close the track loop!
                            trackBreadcrumbs.add(Pair(gate1.x, gate1.y))
                            isDriveAndPaintMode = false
                            soundManager.playFinalLapFanfare()
                            // Reset gates for racing!
                            elements.filterIsInstance<GateElement>().forEach { it.isPassedThisLap = false }
                            activeGateNumber = 1
                            currentLapTimer = 0f
                            _currentLap.value = 1
                        }
                    }
                }
            }
        }

        // 3. Update Timers & Boost States
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

        // 4. Update Combat & Particle System
        particleSystem.update(dt)

        // 5. Update all elements and handle collisions
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

            // Scan for Rocket Lock-On Target (Items & obstacles ONLY - never lock onto gates!)
            if (elem.type == ArElementType.ITEM_BOX || elem.type == ArElementType.BANANA) {
                if (rel.yRel in 0.8f..4.0f && kotlin.math.abs(rel.xRel) < 0.35f) {
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
                if (isDriveAndPaintMode) {
                    // In Drive & Paint mode, gates are waypoints being established
                    if (!elem.isPassedThisLap) {
                        elem.isPassedThisLap = true
                        soundManager.playCountdownGo()
                    }
                    return
                }

                if (!elem.isPassedThisLap && elem.gateNumber == activeGateNumber) {
                    if (elem.isFinishLine) {
                        // Prevent instant re-trigger if kart is lingering inside gate
                        if (currentLapTimer < 3.5f && _currentLap.value > 1) {
                            return
                        }

                        elem.isPassedThisLap = true
                        soundManager.playCountdownGo()
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
                            activeGateNumber = if (isDynamicRoomArcade) 1 else 2
                            if (isDynamicRoomArcade) {
                                spawnNextDynamicGate(1)
                            } else {
                                // Reset all other gates for next lap
                                elements.filterIsInstance<GateElement>().forEach {
                                    if (!it.isFinishLine) it.isPassedThisLap = false
                                }
                            }
                        }
                    } else {
                        elem.isPassedThisLap = true
                        soundManager.playCountdownGo()
                        val allGates = elements.filterIsInstance<GateElement>()
                        val maxGate = allGates.maxOfOrNull { it.gateNumber } ?: 1
                        if (activeGateNumber < maxGate) {
                            activeGateNumber++
                            if (isDynamicRoomArcade) {
                                spawnNextDynamicGate(activeGateNumber)
                            }
                        } else {
                            // All checkpoints passed! Now ready for finish line!
                            activeGateNumber = 1
                            allGates.find { it.isFinishLine }?.isPassedThisLap = false
                        }
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
        // Rockets fire dead-center straight ahead; miniguns alternate left/right bumpers
        val lateralOffset = if (isRocket) 0.0f else (if (bumperLeft) -0.14f else 0.14f)
        val spawnX = tracker.x + (cos(h) * lateralOffset) + (sin(h) * 0.35f)
        val spawnY = tracker.y - (sin(h) * lateralOffset) + (cos(h) * 0.35f)

        if (!isRocket) {
            // Muzzle flash at bumper
            val bumperRelX = lateralOffset
            val bumperRelY = 0.28f
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
        ).apply {
            onEmitTrail = { sx, sy ->
                particleSystem.emitRocketTrail(sx, sy)
            }
        }
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

    private fun renderTrackRibbon(
        canvas: Canvas,
        viewWidth: Float,
        viewHeight: Float,
        pitchDeg: Float,
        rollDeg: Float,
        isLeftEye: Boolean?
    ) {
        val pts = trackBreadcrumbs
        if (pts.size < 2) return

        val halfW = 0.32f // ~64cm total track width

        for (i in 0 until pts.size - 1) {
            val pA = pts[i]
            val pB = pts[i + 1]

            val relA = tracker.toRelative(pA.first, pA.second)
            val relB = tracker.toRelative(pB.first, pB.second)

            // Skip if both points are behind the camera or too far away
            if (relA.yRel < 0.10f && relB.yRel < 0.10f) continue
            if (relA.distance > 9.0f && relB.distance > 9.0f) continue

            val yA = relA.yRel.coerceAtLeast(0.14f)
            val yB = relB.yRel.coerceAtLeast(0.14f)

            val projAL = ArGeometry.project(relA.xRel - halfW, yA, 0f, viewWidth, viewHeight, pitchDeg, rollDeg, isLeftEye)
            val projAR = ArGeometry.project(relA.xRel + halfW, yA, 0f, viewWidth, viewHeight, pitchDeg, rollDeg, isLeftEye)
            val projBL = ArGeometry.project(relB.xRel - halfW, yB, 0f, viewWidth, viewHeight, pitchDeg, rollDeg, isLeftEye)
            val projBR = ArGeometry.project(relB.xRel + halfW, yB, 0f, viewWidth, viewHeight, pitchDeg, rollDeg, isLeftEye)

            val quad = android.graphics.Path().apply {
                moveTo(projAL.screenX, projAL.screenY)
                lineTo(projAR.screenX, projAR.screenY)
                lineTo(projBR.screenX, projBR.screenY)
                lineTo(projBL.screenX, projBL.screenY)
                close()
            }
            canvas.drawPath(quad, trackRibbonFillPaint)
            canvas.drawLine(projAL.screenX, projAL.screenY, projBL.screenX, projBL.screenY, trackRibbonBorderPaint)
            canvas.drawLine(projAR.screenX, projAR.screenY, projBR.screenX, projBR.screenY, trackRibbonBorderPaint)

            // Dashed neon centerline
            if (i % 2 == 0) {
                val projAC = ArGeometry.project(relA.xRel, yA, 0f, viewWidth, viewHeight, pitchDeg, rollDeg, isLeftEye)
                val projBC = ArGeometry.project(relB.xRel, yB, 0f, viewWidth, viewHeight, pitchDeg, rollDeg, isLeftEye)
                canvas.drawLine(projAC.screenX, projAC.screenY, projBC.screenX, projBC.screenY, trackRibbonCenterPaint)
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

        // Render Drive & Paint track ribbon FIRST (on the floor plane underneath objects)
        if (trackBreadcrumbs.size >= 2) {
            renderTrackRibbon(canvas, viewWidth, viewHeight, pitchDeg, rollDeg, isLeftEye)
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

            // Calculate perspective 3D tail position for minigun tracers
            if (elem is ProjectileElement && !elem.isRocket) {
                val tailRel = tracker.toRelative(elem.tailX, elem.tailY)
                val tailProj = ArGeometry.project(
                    xRel = tailRel.xRel,
                    yRel = tailRel.yRel.coerceAtLeast(0.12f),
                    zRel = elem.z,
                    viewWidth = viewWidth,
                    viewHeight = viewHeight,
                    pitchDeg = pitchDeg,
                    rollDeg = rollDeg,
                    isLeftEye = isLeftEye
                )
                elem.tailScreenX = tailProj.screenX
                elem.tailScreenY = tailProj.screenY
            }

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
