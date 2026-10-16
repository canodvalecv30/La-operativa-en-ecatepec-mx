package com.example.game.engine

import android.content.Context
import androidx.compose.runtime.*
import com.example.game.gl.CameraMode
import com.example.game.gl.GameRenderer
import kotlinx.coroutines.*
import kotlin.math.sqrt

class GameEngine(
    val context: Context,
    val prefs: GamePreferences,
    val mode: GameMode
) {
    val audio = GameAudio(context, prefs)

    // Player car specs & upgrades
    private val carSpec: CarSpec = CarCatalog.CARS.find { it.id == prefs.selectedCarId } ?: CarCatalog.CARS[0]
    val carColorHex = prefs.getCarColor(carSpec.id, carSpec.defaultColorHex)

    val carPhysics = CarPhysics(
        spec = carSpec,
        engineUpgrade = prefs.getUpgradeLevel(carSpec.id, "engine"),
        handlingUpgrade = prefs.getUpgradeLevel(carSpec.id, "handling"),
        turboUpgrade = prefs.getUpgradeLevel(carSpec.id, "turbo"),
        brakesUpgrade = prefs.getUpgradeLevel(carSpec.id, "brakes")
    )

    val characterPhysics = CharacterPhysics()

    var activeMap by mutableStateOf(prefs.selectedMap)

    var trackWorld = TrackWorld(mode, activeMap)

    // State: Driving or On-Foot
    var isOnFoot by mutableStateOf(false)
    var canEnterCar by mutableStateOf(false)

    val renderer = GameRenderer(
        getCarPhysics = { carPhysics },
        getTrackWorld = { trackWorld },
        getCarColorHex = { prefs.getCarColor(carSpec.id, carSpec.defaultColorHex) },
        getCharacterPhysics = { characterPhysics },
        getIsOnFoot = { isOnFoot },
        getGraphicsQuality = { prefs.graphicsQuality },
        getCarDecal = { prefs.getCarDecal(carSpec.id) },
        getCarDecalColor = { prefs.getCarDecalColor(carSpec.id) },
        getCarRimColor = { prefs.getCarRimColor(carSpec.id) },
        getCarCaliperColor = { prefs.getCarCaliperColor(carSpec.id) },
        getCharacterSkin = { prefs.characterSkin },
        getMap = { activeMap }
    )

    // Vehicle Driving Inputs
    var inputThrottle = 0f
    var inputBrake = 0f
    var inputSteer = 0f
    var inputHandbrake = false
    var inputNitro = false

    // On-Foot Character Inputs
    var inputMoveX = 0f // -1 to 1
    var inputMoveZ = 0f // -1 to 1
    var inputSprint = false

    // Game state observables for Compose HUD
    var isPaused by mutableStateOf(false)
    var isGameOver by mutableStateOf(false)

    var currentSpeedKmH by mutableStateOf(0f)
    var currentRpm by mutableStateOf(1000f)
    var currentGear by mutableStateOf(1)
    var nitroPercent by mutableStateOf(100f)
    var isDrifting by mutableStateOf(false)
    var driftScore by mutableStateOf(0)
    var currentDriftCombo by mutableStateOf(0f)
    var totalScore by mutableStateOf(0)
    var coinsEarned by mutableStateOf(0)
    var distanceMeters by mutableStateOf(0f)
    var timeRemainingSeconds by mutableStateOf(90f) // For Time Attack & Drift Challenge

    var currentEventNotice by mutableStateOf<EventNotice?>(null)
    private var noticeClearTime = 0L

    private var gameJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        carPhysics.reset()
        isOnFoot = false
        canEnterCar = false
        audio.start()
        isPaused = false
        isGameOver = false

        gameJob?.cancel()
        gameJob = scope.launch {
            var lastTime = System.nanoTime()

            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceIn(0.005f, 0.05f)
                lastTime = now

                if (!isPaused && !isGameOver) {
                    tick(dt)
                }

                delay(16) // ~60 FPS
            }
        }
    }

    private fun tick(dt: Float) {
        if (isOnFoot) {
            // ON-FOOT CHARACTER SIMULATION
            characterPhysics.update(
                inputX = inputMoveX,
                inputZ = inputMoveZ,
                sprint = inputSprint,
                dt = dt
            )

            canEnterCar = characterPhysics.canEnterCar(carPhysics)

            // Audio idle engine purr
            audio.targetRpm = 800f
            audio.throttle = 0f
            audio.nitroActive = false
            audio.onDrift(0f)

            // Check coin pickups on foot
            for (coin in trackWorld.bonusCoins) {
                if (!coin.collected) {
                    val cdx = characterPhysics.posX - coin.posX
                    val cdy = characterPhysics.posY - coin.posY
                    val cdz = characterPhysics.posZ - coin.posZ
                    val dist = sqrt(cdx * cdx + cdy * cdy + cdz * cdz)
                    if (dist < 2.5f) {
                        coin.collected = true
                        trackWorld.coinsCollected += 15
                        trackWorld.totalScore += 100
                        audio.vibrate(40)
                        currentEventNotice = EventNotice(
                            type = EventNoticeType.COIN_PICKUP,
                            text = "+15 MONEDAS (A PIE)!",
                            points = 100
                        )
                        noticeClearTime = System.currentTimeMillis() + 1800
                    }
                }
            }

            // Update world pedestrians & cross streets when on foot
            val onFootNotice = trackWorld.update(carPhysics, characterPhysics, true, dt)
            if (onFootNotice != null) {
                currentEventNotice = onFootNotice
                noticeClearTime = System.currentTimeMillis() + 2500
                audio.vibrate(50)
            }

            currentSpeedKmH = if (characterPhysics.isMoving) (if (characterPhysics.isSprinting) 18f else 9f) else 0f
            distanceMeters = characterPhysics.posZ
        } else {
            // VEHICLE DRIVING SIMULATION
            val isOffRoad = carPhysics.posX < -trackWorld.halfRoadWidth || carPhysics.posX > trackWorld.halfRoadWidth

            carPhysics.update(
                throttle = inputThrottle,
                brake = inputBrake,
                steer = inputSteer,
                handbrake = inputHandbrake,
                nitroRequested = inputNitro,
                dt = dt,
                isOffRoad = isOffRoad
            )

            // Update track world, collisions, traffic, pedestrians, cross streets
            val notice = trackWorld.update(carPhysics, characterPhysics, false, dt)
            if (notice != null) {
                currentEventNotice = notice
                noticeClearTime = System.currentTimeMillis() + 1800

                if (notice.type == EventNoticeType.COLLISION || notice.type == EventNoticeType.BARRIER_HIT) {
                    audio.triggerCrash = true
                    renderer.triggerScreenShake(0.6f)
                }
            }

            // Audio updates
            audio.targetRpm = carPhysics.currentRpm
            audio.throttle = inputThrottle
            audio.onDrift(if (carPhysics.isDrifting) (carPhysics.driftAngle / 40f).coerceIn(0f, 1f) else 0f)
            audio.nitroActive = carPhysics.isNitroActive

            currentSpeedKmH = carPhysics.speedKmH
            currentRpm = carPhysics.currentRpm
            currentGear = carPhysics.currentGear
            nitroPercent = carPhysics.nitroFuel
            isDrifting = carPhysics.isDrifting
            driftScore = carPhysics.driftScore
            currentDriftCombo = carPhysics.driftComboTime
            distanceMeters = carPhysics.posZ
        }

        // Auto-clear notice after delay
        if (currentEventNotice != null && System.currentTimeMillis() > noticeClearTime) {
            currentEventNotice = null
        }

        // Mode specific rules
        if (mode == GameMode.TIME_ATTACK || mode == GameMode.DRIFT_MASTER) {
            timeRemainingSeconds = (timeRemainingSeconds - dt).coerceAtLeast(0f)
            if (timeRemainingSeconds <= 0f) {
                endGame()
            }
        }

        totalScore = trackWorld.totalScore + carPhysics.driftScore
        coinsEarned = trackWorld.coinsCollected + (carPhysics.driftScore / 80)
    }

    fun exitVehicle() {
        if (isOnFoot) return
        carPhysics.speedKmH = 0f
        carPhysics.isBraking = true
        characterPhysics.spawnNearCar(carPhysics)
        isOnFoot = true
        canEnterCar = true
        audio.vibrate(60)

        currentEventNotice = EventNotice(
            type = EventNoticeType.CLOSE_CALL,
            text = "¡TE HAS BAJADO DEL COCHE!",
            points = 0
        )
        noticeClearTime = System.currentTimeMillis() + 2000
    }

    fun enterVehicle() {
        if (!isOnFoot) return
        isOnFoot = false
        audio.vibrate(80)

        currentEventNotice = EventNotice(
            type = EventNoticeType.CLOSE_CALL,
            text = "¡AL VOLANTE DEL COCHE!",
            points = 0
        )
        noticeClearTime = System.currentTimeMillis() + 2000
    }

    fun characterJump() {
        if (isOnFoot) {
            characterPhysics.jump()
            audio.vibrate(30)
        }
    }

    fun toggleCamera() {
        renderer.cameraMode = when (renderer.cameraMode) {
            CameraMode.CHASE -> CameraMode.HOOD
            CameraMode.HOOD -> CameraMode.CHASE
            else -> CameraMode.CHASE
        }
    }

    fun resetCarPosition() {
        if (isOnFoot) {
            characterPhysics.spawnNearCar(carPhysics)
        } else {
            carPhysics.posX = 0f
            carPhysics.posY = 0.5f
            carPhysics.headingAngle = 0f
            carPhysics.pitchAngle = 0f
            carPhysics.rollAngle = 0f
            carPhysics.speedKmH = 0f
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun changeMap(newMap: GameMap) {
        prefs.selectedMap = newMap
        activeMap = newMap
        trackWorld = TrackWorld(mode, newMap)
        resetCarPosition()
        currentEventNotice = EventNotice(
            type = EventNoticeType.CLOSE_CALL,
            text = "¡MAPA CARGADO: ${newMap.title.uppercase()}!",
            points = 50
        )
        noticeClearTime = System.currentTimeMillis() + 2500
    }

    fun endGame() {
        isGameOver = true
        // Save coins & high scores
        prefs.coins += coinsEarned

        when (mode) {
            GameMode.TRAFFIC_RUSH -> {
                if (distanceMeters > prefs.bestDistanceRush) {
                    prefs.bestDistanceRush = distanceMeters
                }
            }
            GameMode.DRIFT_MASTER -> {
                if (driftScore > prefs.bestDriftScore) {
                    prefs.bestDriftScore = driftScore
                }
            }
            else -> {}
        }
    }

    fun destroy() {
        gameJob?.cancel()
        audio.stop()
    }
}
