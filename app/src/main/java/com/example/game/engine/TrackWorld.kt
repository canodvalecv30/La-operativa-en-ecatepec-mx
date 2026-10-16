package com.example.game.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

data class TrafficVehicle(
    var id: Int,
    var posX: Float,
    var posZ: Float,
    var speedKmH: Float,
    var targetLaneX: Float,
    var vehicleType: Int, // 0 = Sedan, 1 = SUV, 2 = Sport, 3 = Truck
    var colorHex: Long,
    var isHit: Boolean = false,
    var hitTimer: Float = 0f
)

data class StuntRamp(
    val posX: Float,
    val posZ: Float,
    val width: Float = 6f,
    val length: Float = 10f,
    val height: Float = 3.5f
)

data class BonusCoin(
    val posX: Float,
    val posY: Float,
    val posZ: Float,
    var collected: Boolean = false
)

data class Projectile(
    val id: Int,
    var posX: Float,
    var posY: Float,
    var posZ: Float,
    var dirX: Float,
    var dirY: Float,
    var dirZ: Float,
    var speed: Float,
    var life: Float = 0f,
    var maxLife: Float = 2.2f,
    var colorHex: Long = 0xFFFFD700,
    var damage: Float = 50f,
    var isLaser: Boolean = false
)

data class TargetDrone(
    val id: Int,
    var posX: Float,
    var posY: Float,
    var posZ: Float,
    var rotationAngle: Float = 0f,
    var hp: Float = 100f,
    var maxHp: Float = 100f,
    var isDestroyed: Boolean = false,
    var hoverPhase: Float = 0f,
    var swaySpeed: Float = 1.5f
)

data class StuntCrate(
    val id: Int,
    var posX: Float,
    var posY: Float = 0.55f,
    var posZ: Float,
    var hp: Float = 40f,
    var isDestroyed: Boolean = false
)

enum class WeaponType(
    val id: String,
    val displayName: String,
    val fireRateSeconds: Float,
    val damage: Float,
    val projectileSpeed: Float,
    val colorHex: Long,
    val maxAmmo: Int,
    val isLaser: Boolean,
    val recoilForce: Float,
    val description: String
) {
    TACTICAL_9MM(
        id = "pistol_9mm",
        displayName = "Pistola 9mm Táctica",
        fireRateSeconds = 0.20f,
        damage = 50f,
        projectileSpeed = 105f,
        colorHex = 0xFFFFC107,
        maxAmmo = 15,
        isLaser = false,
        recoilForce = 0.5f,
        description = "Semiautomática precisa y ligera. Alta fiabilidad y cadencia."
    ),
    MAGNUM_44(
        id = "magnum_44",
        displayName = "Magnum .44 Desert",
        fireRateSeconds = 0.42f,
        damage = 120f,
        projectileSpeed = 125f,
        colorHex = 0xFFFF5722,
        maxAmmo = 6,
        isLaser = false,
        recoilForce = 1.0f,
        description = "Devastador calibre pesado. Destruye drones y cajas de un solo tiro."
    ),
    CYBER_BLASTER(
        id = "cyber_blaster",
        displayName = "Blaster Plasma Neón",
        fireRateSeconds = 0.12f,
        damage = 36f,
        projectileSpeed = 140f,
        colorHex = 0xFF00E5FF,
        maxAmmo = 32,
        isLaser = true,
        recoilForce = 0.3f,
        description = "Pistola de energía con proyectiles continuos de plasma cyan luminoso."
    )
}

enum class PedestrianState {
    WALKING,
    CROSSING,
    DODGING,
    WAVING
}

data class Pedestrian(
    val id: Int,
    var posX: Float,
    var posY: Float = 0.25f, // Elevated on sidewalk by default
    var posZ: Float,
    var speed: Float,
    var headingAngle: Float,
    var walkPhase: Float = Random.nextFloat() * 10f,
    var isCrossing: Boolean = false,
    var crossTargetX: Float = 0f,
    var outfitColorHex: Long,
    var skinType: Int = 0, // 0 = Navy suit, 1 = Sport runner, 2 = Casual jacket, 3 = Chic/Tourist
    var state: PedestrianState = PedestrianState.WALKING,
    var reactionTimer: Float = 0f,
    var headTurnAngle: Float = 0f
)

data class CrossStreet(
    val id: Int,
    val posZ: Float,
    val name: String,
    val hasTrafficLight: Boolean = true,
    var trafficLightState: Int = 0 // 0 = Green, 1 = Yellow, 2 = Red
)

class TrackWorld(
    val mode: GameMode,
    val map: GameMap = GameMap.NEON_CITY
) {
    // Road & Sidewalk dimensions
    val roadWidth = 24f // 4 lanes, approx 6m each
    val halfRoadWidth = roadWidth / 2f
    val sidewalkWidth = 4.5f
    val lanes = floatArrayOf(-9f, -3f, 3f, 9f)

    // Sidewalk centers
    val leftSidewalkX = -halfRoadWidth - (sidewalkWidth / 2f) // -14.25f
    val rightSidewalkX = halfRoadWidth + (sidewalkWidth / 2f) // +14.25f

    // Traffic cars
    val trafficCars = mutableListOf<TrafficVehicle>()
    private var nextCarId = 1

    // Cross Streets / Intersections
    val crossStreets = mutableListOf<CrossStreet>()
    private val streetNames = listOf(
        "Av. de la Libertad",
        "Gran Vía Apex",
        "Boulevard del Sol",
        "Paseo Marítimo",
        "Av. de las Palmeras",
        "Calle 5ª Sinfonía",
        "Distrito Portuario",
        "Rambla de los Pilotos",
        "Av. del Circuito",
        "Calle Nueva Marina"
    )

    // Pedestrians
    val pedestrians = mutableListOf<Pedestrian>()
    private var nextPedestrianId = 1

    // Ramps and Stunts
    val ramps = mutableListOf<StuntRamp>()

    // Collectible Coins
    val bonusCoins = mutableListOf<BonusCoin>()

    // World statistics
    var distanceTraveledMeters = 0f
    var closeCallsCount = 0
    var pedestrianDodgesCount = 0
    var coinsCollected = 0
    var totalScore = 0

    init {
        generateInitialWorld()
    }

    private fun generateInitialWorld() {
        trafficCars.clear()
        crossStreets.clear()
        pedestrians.clear()
        ramps.clear()
        bonusCoins.clear()

        // Generate Cross-Streets every 140 meters
        val crossStreetInterval = 140f
        for (i in 1..25) {
            val z = i * crossStreetInterval
            val name = streetNames[(i - 1) % streetNames.size]
            crossStreets.add(
                CrossStreet(
                    id = i,
                    posZ = z,
                    name = name,
                    hasTrafficLight = true,
                    trafficLightState = (i % 3)
                )
            )
        }

        // Place ramps based on map and mode
        val rampInterval = when (map) {
            GameMap.STUNT_DISTRICT -> if (mode == GameMode.FREE_ROAM) 70f else 110f
            GameMap.COASTAL_SUNSET -> if (mode == GameMode.FREE_ROAM) 140f else 220f
            else -> if (mode == GameMode.FREE_ROAM) 120f else 240f
        }

        val rampCount = if (map == GameMap.STUNT_DISTRICT) 30 else 18
        for (i in 1..rampCount) {
            val z = i * rampInterval
            val laneX = if (i % 2 == 0) 3f else -3f
            val rampH = if (map == GameMap.STUNT_DISTRICT) 4.8f else 3.5f
            ramps.add(StuntRamp(posX = laneX, posZ = z, height = rampH))

            // Add floating coins over ramp for stunt reward
            val coinCount = if (map == GameMap.STUNT_DISTRICT) 6 else 4
            for (c in 1..coinCount) {
                bonusCoins.add(BonusCoin(posX = laneX, posY = 2f + c * 0.85f, posZ = z + c * 3.5f))
            }
        }

        // Spawn initial pedestrians along sidewalks & crosswalks
        for (i in 0..20) {
            spawnInitialPedestrian(minZ = 15f + i * 20f)
        }

        // Spawn initial traffic
        if (mode == GameMode.TRAFFIC_RUSH || mode == GameMode.FREE_ROAM) {
            for (i in 1..15) {
                spawnTrafficCar(minZ = 40f + i * 35f)
            }
        }
    }

    private fun spawnInitialPedestrian(minZ: Float) {
        val onLeft = Random.nextBoolean()
        val sideX = if (onLeft) leftSidewalkX + (Random.nextFloat() - 0.5f) * 1.5f else rightSidewalkX + (Random.nextFloat() - 0.5f) * 1.5f
        val walkingForward = Random.nextBoolean()
        val heading = if (walkingForward) 0f else 180f
        val speed = Random.nextFloat() * 0.8f + 1.2f // 1.2 - 2.0 m/s

        val outfitColors = longArrayOf(
            0xFF1E88E5, // Royal blue
            0xFFE53935, // Crimson red
            0xFF43A047, // Forest green
            0xFFFB8C00, // Vibrant amber
            0xFF8E24AA, // Purple
            0xFF37474F, // Dark slate
            0xFFECEFF1, // Crisp white
            0xFFFFD600  // Bright yellow
        )
        val outfit = outfitColors[Random.nextInt(outfitColors.size)]
        val skinType = Random.nextInt(4)

        // Check if spawning near an intersection crosswalk to cross street
        val nearestCross = crossStreets.find { abs(it.posZ - minZ) < 8f }
        val isCrossing = nearestCross != null && Random.nextFloat() < 0.45f

        val pedX = if (isCrossing) (if (onLeft) leftSidewalkX else rightSidewalkX) else sideX
        val pedTargetX = if (isCrossing) (if (onLeft) rightSidewalkX else leftSidewalkX) else pedX
        val pedHeading = if (isCrossing) (if (onLeft) 90f else -90f) else heading

        pedestrians.add(
            Pedestrian(
                id = nextPedestrianId++,
                posX = pedX,
                posY = 0.25f,
                posZ = minZ,
                speed = speed,
                headingAngle = pedHeading,
                isCrossing = isCrossing,
                crossTargetX = pedTargetX,
                outfitColorHex = outfit,
                skinType = skinType,
                state = if (isCrossing) PedestrianState.CROSSING else PedestrianState.WALKING
            )
        )
    }

    private fun spawnTrafficCar(minZ: Float) {
        val laneIndex = Random.nextInt(lanes.size)
        val laneX = lanes[laneIndex]
        val vType = Random.nextInt(4)
        val speed = when (vType) {
            3 -> Random.nextFloat() * 25f + 65f // Truck
            1 -> Random.nextFloat() * 30f + 85f // SUV
            0 -> Random.nextFloat() * 35f + 95f // Sedan
            else -> Random.nextFloat() * 40f + 115f // Sport
        }
        val colors = longArrayOf(0xFF1976D2, 0xFF388E3C, 0xFFFBC02D, 0xFFD32F2F, 0xFF7B1FA2, 0xFF616161, 0xFFE0E0E0)
        val carColor = colors[Random.nextInt(colors.size)]

        trafficCars.add(
            TrafficVehicle(
                id = nextCarId++,
                posX = laneX,
                posZ = minZ,
                speedKmH = speed,
                targetLaneX = laneX,
                vehicleType = vType,
                colorHex = carColor
            )
        )
    }

    fun update(
        playerCar: CarPhysics,
        playerChar: CharacterPhysics?,
        isOnFoot: Boolean,
        dt: Float
    ): EventNotice? {
        var notice: EventNotice? = null
        distanceTraveledMeters = if (isOnFoot && playerChar != null) playerChar.posZ else playerCar.posZ

        val activeX = if (isOnFoot && playerChar != null) playerChar.posX else playerCar.posX
        val activeZ = if (isOnFoot && playerChar != null) playerChar.posZ else playerCar.posZ
        val activeY = if (isOnFoot && playerChar != null) playerChar.posY else playerCar.posY

        // 1. Update Traffic Lights across cross streets
        for (cross in crossStreets) {
            val cycleTime = (System.currentTimeMillis() / 4500L + cross.id) % 3
            cross.trafficLightState = cycleTime.toInt()
        }

        // 2. Update Pedestrians
        val pedIterator = pedestrians.iterator()
        while (pedIterator.hasNext()) {
            val ped = pedIterator.next()

            // Despawn if far behind player
            if (activeZ - ped.posZ > 65f) {
                pedIterator.remove()
                continue
            }

            // Reaction recovery
            if (ped.state == PedestrianState.DODGING || ped.state == PedestrianState.WAVING) {
                ped.reactionTimer += dt
                if (ped.reactionTimer > 2.2f) {
                    ped.state = PedestrianState.WALKING
                    ped.reactionTimer = 0f
                    ped.headTurnAngle = 0f
                }
            }

            // Normal Walking Simulation
            if (ped.state == PedestrianState.WALKING || ped.state == PedestrianState.CROSSING) {
                ped.walkPhase += ped.speed * dt * 8.5f

                if (ped.isCrossing) {
                    val stepDir = if (ped.crossTargetX > ped.posX) 1f else -1f
                    ped.posX += stepDir * ped.speed * dt
                    ped.headingAngle = if (stepDir > 0) 90f else -90f
                    ped.posY = 0.05f // On road surface while crossing

                    // Arrived at sidewalk
                    if (abs(ped.posX - ped.crossTargetX) < 0.6f) {
                        ped.isCrossing = false
                        ped.state = PedestrianState.WALKING
                        ped.headingAngle = if (Random.nextBoolean()) 0f else 180f
                        ped.posY = 0.25f // Back on elevated sidewalk
                    }
                } else {
                    // Walking along sidewalk
                    val dirZ = if (ped.headingAngle in -45f..45f) 1f else -1f
                    ped.posZ += dirZ * ped.speed * dt
                    ped.posY = 0.25f // On elevated sidewalk
                }
            }

            // Distance to player
            val pdx = activeX - ped.posX
            val pdz = activeZ - ped.posZ
            val pdist = sqrt(pdx * pdx + pdz * pdz)

            // Interaction with player On-Foot
            if (isOnFoot && pdist < 2.5f && ped.state != PedestrianState.DODGING) {
                ped.state = PedestrianState.WAVING
                ped.reactionTimer = 0f
                // Turn head toward player
                val angleToPlayer = Math.toDegrees(kotlin.math.atan2(pdx.toDouble(), pdz.toDouble())).toFloat()
                ped.headTurnAngle = (angleToPlayer - ped.headingAngle).coerceIn(-60f, 60f)

                if (notice == null && Random.nextFloat() < 0.08f) {
                    val greetings = listOf(
                        "¡HOLA PILOTO! ¡VAYA COCHAZO TIENES!",
                        "¡BUENA CONDUCCIÓN POR LA CIUDAD!",
                        "¡ESE MOTOR SUENA INCREÍBLE, CAMPEÓN!",
                        "¡QUÉ AUTÉNTICA MÁQUINA DE CARRERAS!"
                    )
                    notice = EventNotice(
                        type = EventNoticeType.CLOSE_CALL,
                        text = "PEATÓN: \"${greetings[Random.nextInt(greetings.size)]}\"",
                        points = 25
                    )
                    totalScore += 25
                }
            }

            // Evasion / Dodge if player car is speeding dangerously close
            if (!isOnFoot && pdist < 4.2f && playerCar.speedKmH > 25f && ped.state != PedestrianState.DODGING) {
                ped.state = PedestrianState.DODGING
                ped.reactionTimer = 0f

                // Jump away from car towards sidewalk edge
                val dodgeDir = if (playerCar.posX > ped.posX) -1.2f else 1.2f
                ped.posX += dodgeDir

                pedestrianDodgesCount++
                val bonus = 75
                totalScore += bonus
                coinsCollected += 3
                notice = EventNotice(
                    type = EventNoticeType.CLOSE_CALL,
                    text = "¡PEATÓN ESQUIVADO AL LÍMITE! +$bonus PTS",
                    points = bonus
                )
            }
        }

        // Keep pedestrians populated ahead of player
        val furthestPedZ = pedestrians.maxOfOrNull { it.posZ } ?: activeZ
        if (pedestrians.size < 18) {
            val spawnZ = maxOf(furthestPedZ + Random.nextFloat() * 30f + 18f, activeZ + 75f)
            spawnInitialPedestrian(spawnZ)
        }

        // 3. Update Traffic vehicles
        val carIterator = trafficCars.iterator()
        while (carIterator.hasNext()) {
            val traffic = carIterator.next()

            if (traffic.isHit) {
                traffic.hitTimer += dt
                traffic.posX += 5f * dt
                traffic.speedKmH = (traffic.speedKmH - 30f * dt).coerceAtLeast(0f)
            } else {
                val distBehind = playerCar.posZ - traffic.posZ
                val distAhead = traffic.posZ - playerCar.posZ

                if (distBehind > 70f) {
                    carIterator.remove()
                    continue
                }

                if (Random.nextFloat() < 0.003f) {
                    val newLane = lanes[Random.nextInt(lanes.size)]
                    traffic.targetLaneX = newLane
                }

                traffic.posX += (traffic.targetLaneX - traffic.posX) * min(1f, 2.5f * dt)

                val trafficMps = (traffic.speedKmH * 1000f) / 3600f
                traffic.posZ += trafficMps * dt

                // Collision detection with player car
                if (!isOnFoot) {
                    val dx = abs(playerCar.posX - traffic.posX)
                    val dz = abs(playerCar.posZ - traffic.posZ)
                    val dy = abs(playerCar.posY)

                    if (dx < 2.4f && dz < 4.5f && dy < 1.8f) {
                        traffic.isHit = true
                        playerCar.speedKmH *= 0.45f
                        notice = EventNotice(
                            type = EventNoticeType.COLLISION,
                            text = "¡CHOQUE CON TRÁFICO!",
                            points = 0
                        )
                    } else if (dx < 3.8f && dz < 3.5f && playerCar.speedKmH > 85f && distAhead < 3f && distBehind < 3f) {
                        closeCallsCount++
                        val pts = 150
                        totalScore += pts
                        coinsCollected += 5
                        notice = EventNotice(
                            type = EventNoticeType.CLOSE_CALL,
                            text = "¡ADELANTAMIENTO AL LÍMITE! +$pts PTS",
                            points = pts
                        )
                    }
                }
            }
        }

        // Keep traffic populated
        val furthestCarZ = trafficCars.maxOfOrNull { it.posZ } ?: playerCar.posZ
        if (trafficCars.size < 14 && mode != GameMode.TIME_ATTACK) {
            val spawnZ = maxOf(furthestCarZ + Random.nextFloat() * 45f + 30f, playerCar.posZ + 90f)
            spawnTrafficCar(spawnZ)
        }

        // 4. Stunt Ramps
        if (!isOnFoot) {
            for (ramp in ramps) {
                val rdx = abs(playerCar.posX - ramp.posX)
                val rdz = playerCar.posZ - ramp.posZ

                if (rdx < ramp.width / 2f && rdz in 0f..ramp.length) {
                    val rampProgress = rdz / ramp.length
                    val rampHeightAtPoint = rampProgress * ramp.height
                    if (playerCar.posY < rampHeightAtPoint) {
                        playerCar.posY = rampHeightAtPoint
                        playerCar.pitchAngle = -15f
                    }

                    if (rampProgress > 0.9f && !playerCar.isAirborne && playerCar.speedKmH > 40f) {
                        val jumpMultiplier = if (map == GameMap.STUNT_DISTRICT) 1.25f else 1.0f
                        val jumpForce = ((playerCar.speedKmH / 18f) * jumpMultiplier).coerceIn(8f, 26f)
                        playerCar.jump(jumpForce)
                        notice = EventNotice(
                            type = EventNoticeType.STUNT_JUMP,
                            text = "¡MEGA SALTO ACROBÁTICO! +350 PTS",
                            points = 350
                        )
                        totalScore += 350
                        coinsCollected += 12
                    }
                }
            }
        }

        // 5. Collect Coins
        val checkX = activeX
        val checkY = activeY
        val checkZ = activeZ

        for (coin in bonusCoins) {
            if (!coin.collected) {
                val cdx = checkX - coin.posX
                val cdy = checkY - coin.posY
                val cdz = checkZ - coin.posZ
                val dist = sqrt(cdx * cdx + cdy * cdy + cdz * cdz)
                if (dist < 3.2f) {
                    coin.collected = true
                    coinsCollected += 15
                    totalScore += 100
                    if (!isOnFoot) {
                        playerCar.nitroFuel = (playerCar.nitroFuel + 18f).coerceAtMost(100f)
                    }
                    notice = EventNotice(
                        type = EventNoticeType.COIN_PICKUP,
                        text = "+15 MONEDAS / +NITRO!",
                        points = 100
                    )
                }
            }
        }

        // 6. Barrier and Sidewalk Curbs
        if (!isOnFoot) {
            val maxRoadX = halfRoadWidth + sidewalkWidth + 1.2f
            if (playerCar.posX < -maxRoadX) {
                playerCar.bounceFromBarrier(-maxRoadX)
                notice = EventNotice(type = EventNoticeType.BARRIER_HIT, text = "¡IMPACTO CON BARRERA!", points = 0)
            } else if (playerCar.posX > maxRoadX) {
                playerCar.bounceFromBarrier(maxRoadX)
                notice = EventNotice(type = EventNoticeType.BARRIER_HIT, text = "¡IMPACTO CON BARRERA!", points = 0)
            }
        }

        return notice
    }
}

enum class EventNoticeType {
    CLOSE_CALL,
    STUNT_JUMP,
    COLLISION,
    BARRIER_HIT,
    COIN_PICKUP,
    DRIFT_COMBO
}

data class EventNotice(
    val type: EventNoticeType,
    val text: String,
    val points: Int,
    val timestamp: Long = System.currentTimeMillis()
)
