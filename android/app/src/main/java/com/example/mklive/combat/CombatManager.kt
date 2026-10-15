package com.example.mklive.combat

import com.example.mklive.ar.model.ArElement
import com.example.mklive.audio.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Combat & Weapons Manager.
 * Handles active inventory, item roulette, Minigun rapid fire, Lock-On missile tracking,
 * and combat sound/particle coordination.
 */
class CombatManager(
    private val soundManager: SoundManager,
    val particleSystem: ParticleSystem
) {

    private val _activeWeapon = MutableStateFlow(WeaponType.NONE)
    val activeWeapon: StateFlow<WeaponType> = _activeWeapon.asStateFlow()

    private val _ammo = MutableStateFlow(0)
    val ammo: StateFlow<Int> = _ammo.asStateFlow()

    private val _isRouletteActive = MutableStateFlow(false)
    val isRouletteActive: StateFlow<Boolean> = _isRouletteActive.asStateFlow()

    private val _rouletteItem = MutableStateFlow(WeaponType.MUSHROOM_BOOST)
    val rouletteItem: StateFlow<WeaponType> = _rouletteItem.asStateFlow()

    private val _lockOnTarget = MutableStateFlow<ArElement?>(null)
    val lockOnTarget: StateFlow<ArElement?> = _lockOnTarget.asStateFlow()

    private val _isMinigunFiring = MutableStateFlow(false)
    val isMinigunFiring: StateFlow<Boolean> = _isMinigunFiring.asStateFlow()

    // Callbacks to ArEngine
    var onSpawnProjectile: ((isRocket: Boolean, target: ArElement?, bumperLeft: Boolean) -> Unit)? = null
    var onDropBanana: (() -> Unit)? = null
    var onTriggerBoost: (() -> Unit)? = null
    var onDetonateBobomb: (() -> Unit)? = null

    private var rouletteJob: Job? = null
    private var minigunJob: Job? = null
    private var bumperSide = false // Alternates left/right bumper
    private val random = Random(42)

    private val availableItems = listOf(
        WeaponType.ROCKET,
        WeaponType.MINIGUN,
        WeaponType.BANANA,
        WeaponType.BOBOMB,
        WeaponType.MUSHROOM_BOOST,
        WeaponType.GREEN_SHELL
    )

    fun startRoulette(scope: CoroutineScope) {
        if (_isRouletteActive.value) return
        _isRouletteActive.value = true

        rouletteJob?.cancel()
        rouletteJob = scope.launch(Dispatchers.Default) {
            val totalTicks = 24
            for (i in 0 until totalTicks) {
                delay(75L + (i * 5L)) // Gradually slows down
                val nextItem = availableItems[random.nextInt(availableItems.size)]
                _rouletteItem.value = nextItem
                soundManager.playRouletteTick()
            }

            // Award final item
            val finalItem = availableItems[random.nextInt(availableItems.size)]
            _activeWeapon.value = finalItem
            _ammo.value = when (finalItem) {
                WeaponType.MINIGUN -> 99
                WeaponType.ROCKET -> 3
                WeaponType.BANANA -> 3
                WeaponType.BOBOMB -> 1
                WeaponType.MUSHROOM_BOOST -> 1
                WeaponType.GREEN_SHELL -> 3
                WeaponType.NONE -> 0
            }
            _isRouletteActive.value = false
            soundManager.playItemAwarded()
        }
    }

    fun setLockOnTarget(target: ArElement?) {
        if (_lockOnTarget.value != target) {
            _lockOnTarget.value = target
            if (target != null) {
                soundManager.playLockOn()
            }
        }
    }

    fun startMinigun(scope: CoroutineScope) {
        if (_isMinigunFiring.value) return
        _isMinigunFiring.value = true

        minigunJob?.cancel()
        minigunJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isMinigunFiring.value) {
                // 18 Hz rapid fire (~55 ms)
                bumperSide = !bumperSide
                soundManager.playMinigunShot()
                onSpawnProjectile?.invoke(false, null, bumperSide)
                delay(55L)
            }
        }
    }

    fun stopMinigun() {
        _isMinigunFiring.value = false
        minigunJob?.cancel()
        minigunJob = null
    }

    fun fireRocket(target: ArElement? = _lockOnTarget.value) {
        soundManager.playRocketLaunch()
        onSpawnProjectile?.invoke(true, target, false)
        decrementAmmo()
    }

    fun dropBanana() {
        soundManager.playBananaDrop()
        onDropBanana?.invoke()
        decrementAmmo()
    }

    fun throwBobomb() {
        soundManager.playExplosion()
        onDetonateBobomb?.invoke()
        decrementAmmo()
    }

    fun useBoost() {
        soundManager.playBoostTurbo()
        onTriggerBoost?.invoke()
        decrementAmmo()
    }

    fun cycleWeapon() {
        val current = _activeWeapon.value
        val nextIdx = (availableItems.indexOf(current) + 1) % availableItems.size
        _activeWeapon.value = availableItems[nextIdx]
        soundManager.playHorn()
    }

    private fun decrementAmmo() {
        val currentAmmo = _ammo.value
        if (currentAmmo > 1) {
            _ammo.value = currentAmmo - 1
        } else if (_activeWeapon.value != WeaponType.MINIGUN) {
            // Revert to minigun after using up special weapon
            _activeWeapon.value = WeaponType.MINIGUN
            _ammo.value = 99
        }
    }
}
