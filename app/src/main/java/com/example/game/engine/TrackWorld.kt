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

class TrackWorld(val mode: GameMode) {
    // Road dimensions
    val roadWidth = 24f // 4 lanes, approx 6m each
    val halfRoadWidth = roadWidth / 2f
    val lanes = floatArrayOf(-9f, -3f, 3f, 9f)

    // Traffic cars
    val trafficCars = mutableListOf<TrafficVehicle>()
    private var nextCarId = 1

    // Ramps and Stunts
    val ramps = mutableListOf<StuntRamp>()

    // Collectible Coins
    val bonusCoins = mutableListOf<BonusCoin>()

    // World statistics
    var distanceTraveledMeters = 0f
    var closeCallsCount = 0
    var coinsCollected = 0
    var totalScore = 0

    init {
        generateInitialWorld()
    }

    private fun generateInitialWorld() {
        trafficCars.clear()
        ramps.clear()
        bonusCoins.clear()

        // Place ramps
        val rampInterval = if (mode == GameMode.FREE_ROAM) 120f else 220f
        for (i in 1..20) {
            val z = i * rampInterval
            val laneX = if (i % 2 == 0) 3f else -3f
            ramps.add(StuntRamp(posX = laneX, posZ = z))

            // Add floating coins over ramp for stunt reward
            for (c in 1..4) {
                bonusCoins.add(BonusCoin(posX = laneX, posY = 2f + c * 0.8f, posZ = z + c * 3.5f))
            }
        }

        // Spawn initial traffic
        if (mode == GameMode.TRAFFIC_RUSH || mode == GameMode.FREE_ROAM) {
            for (i in 1..15) {
                spawnTrafficCar(minZ = 40f + i * 35f)
            }
        }
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

    fun update(playerCar: CarPhysics, dt: Float): EventNotice? {
        var notice: EventNotice? = null
        distanceTraveledMeters = playerCar.posZ

        // Update traffic vehicles
        val carIterator = trafficCars.iterator()
        while (carIterator.hasNext()) {
            val traffic = carIterator.next()

            if (traffic.isHit) {
                traffic.hitTimer += dt
                // Spin/push out of way
                traffic.posX += 5f * dt
                traffic.speedKmH = (traffic.speedKmH - 30f * dt).coerceAtLeast(0f)
            } else {
                // Normal driving
                val distBehind = playerCar.posZ - traffic.posZ
                val distAhead = traffic.posZ - playerCar.posZ

                // Despawn if far behind player
                if (distBehind > 70f) {
                    carIterator.remove()
                    continue
                }

                // Random lane change for excitement
                if (Random.nextFloat() < 0.003f) {
                    val newLane = lanes[Random.nextInt(lanes.size)]
                    traffic.targetLaneX = newLane
                }

                // Smooth lane change
                traffic.posX += (traffic.targetLaneX - traffic.posX) * min(1f, 2.5f * dt)

                // Move forward
                val trafficMps = (traffic.speedKmH * 1000f) / 3600f
                traffic.posZ += trafficMps * dt

                // Collision detection with player
                val dx = abs(playerCar.posX - traffic.posX)
                val dz = abs(playerCar.posZ - traffic.posZ)
                val dy = abs(playerCar.posY)

                if (dx < 2.4f && dz < 4.5f && dy < 1.8f) {
                    // CRASH!
                    traffic.isHit = true
                    playerCar.speedKmH *= 0.45f
                    notice = EventNotice(
                        type = EventNoticeType.COLLISION,
                        text = "¡CHOQUE!",
                        points = 0
                    )
                } else if (dx < 3.8f && dz < 3.5f && playerCar.speedKmH > 85f && distAhead < 3f && distBehind < 3f) {
                    // Close call / Near miss overtake bonus!
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

        // Keep traffic populated ahead of player
        val furthestZ = trafficCars.maxOfOrNull { it.posZ } ?: playerCar.posZ
        if (trafficCars.size < 14 && mode != GameMode.TIME_ATTACK) {
            val spawnZ = maxOf(furthestZ + Random.nextFloat() * 45f + 30f, playerCar.posZ + 90f)
            spawnTrafficCar(spawnZ)
        }

        // Ramp jumps
        for (ramp in ramps) {
            val rdx = abs(playerCar.posX - ramp.posX)
            val rdz = playerCar.posZ - ramp.posZ

            if (rdx < ramp.width / 2f && rdz in 0f..ramp.length) {
                // Climb ramp
                val rampProgress = rdz / ramp.length
                val rampHeightAtPoint = rampProgress * ramp.height
                if (playerCar.posY < rampHeightAtPoint) {
                    playerCar.posY = rampHeightAtPoint
                    playerCar.pitchAngle = -15f // Tilt nose up
                }

                // Launch at the tip
                if (rampProgress > 0.9f && !playerCar.isAirborne && playerCar.speedKmH > 40f) {
                    val jumpForce = (playerCar.speedKmH / 18f).coerceIn(8f, 22f)
                    playerCar.jump(jumpForce)
                    notice = EventNotice(
                        type = EventNoticeType.STUNT_JUMP,
                        text = "¡SALTO ACROBÁTICO! +300 PTS",
                        points = 300
                    )
                    totalScore += 300
                    coinsCollected += 10
                }
            }
        }

        // Collect coins
        for (coin in bonusCoins) {
            if (!coin.collected) {
                val cdx = playerCar.posX - coin.posX
                val cdy = playerCar.posY - coin.posY
                val cdz = playerCar.posZ - coin.posZ
                val dist = sqrt(cdx * cdx + cdy * cdy + cdz * cdz)
                if (dist < 3.2f) {
                    coin.collected = true
                    coinsCollected += 15
                    totalScore += 100
                    playerCar.nitroFuel = (playerCar.nitroFuel + 15f).coerceAtMost(100f)
                    notice = EventNotice(
                        type = EventNoticeType.COIN_PICKUP,
                        text = "+15 MONEDAS / +NITRO!",
                        points = 100
                    )
                }
            }
        }

        // Barrier collisions
        if (playerCar.posX < -halfRoadWidth - 1.5f) {
            playerCar.bounceFromBarrier(-halfRoadWidth - 1.2f)
            notice = EventNotice(type = EventNoticeType.BARRIER_HIT, text = "¡IMPACTO CON BARRERA!", points = 0)
        } else if (playerCar.posX > halfRoadWidth + 1.5f) {
            playerCar.bounceFromBarrier(halfRoadWidth + 1.2f)
            notice = EventNotice(type = EventNoticeType.BARRIER_HIT, text = "¡IMPACTO CON BARRERA!", points = 0)
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
