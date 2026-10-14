package com.example.game.engine

import kotlin.math.*

class CarPhysics(
    val spec: CarSpec,
    val engineUpgrade: Int = 1,
    val handlingUpgrade: Int = 1,
    val turboUpgrade: Int = 1,
    val brakesUpgrade: Int = 1
) {
    // Spatial coordinates (World space: X = right/left, Y = height, Z = forward)
    var posX = 0f
    var posY = 0f
    var posZ = 0f

    // Velocities
    var speedKmH = 0f
    var velocityY = 0f

    // Rotations (in degrees)
    var headingAngle = 0f // Yaw
    var pitchAngle = 0f   // Acceleration / Brake / Ramp pitch
    var rollAngle = 0f    // Cornering body lean
    var steerVisualAngle = 0f // Front wheel steer angle (-35 to +35 deg)
    var wheelSpinAngle = 0f   // Rotation of wheels spinning

    // Physics parameters derived from car specs & upgrades
    val maxSpeed: Float = (spec.baseTopSpeed + (engineUpgrade - 1) * 18f) // km/h
    val accelerationPower: Float = (spec.baseAcceleration + (engineUpgrade - 1) * 1.5f + (turboUpgrade - 1) * 1.2f)
    val handlingSens: Float = (spec.baseHandling + (handlingUpgrade - 1) * 1.5f)
    val brakePower: Float = (spec.baseBraking + (brakesUpgrade - 1) * 2.0f)
    val driftFactor: Float = spec.baseDriftFactor

    // Powertrain state
    var currentGear = 1
    var currentRpm = 1000f
    var isReverse = false
    var isBraking = false
    var isDrifting = false
    var driftAngle = 0f
    var driftComboTime = 0f
    var driftScore = 0

    // Nitro
    var nitroFuel = 100f // 0 to 100%
    var isNitroActive = false

    // Airtime / Jumps
    var isAirborne = false
    var airTime = 0f

    // Gear ratios for top speeds per gear (km/h)
    private val gearMaxSpeeds = floatArrayOf(0f, 65f, 115f, 170f, 225f, 280f, 380f)

    fun reset(startZ: Float = 0f, startX: Float = 0f) {
        posX = startX
        posY = 0f
        posZ = startZ
        speedKmH = 0f
        velocityY = 0f
        headingAngle = 0f
        pitchAngle = 0f
        rollAngle = 0f
        steerVisualAngle = 0f
        wheelSpinAngle = 0f
        currentGear = 1
        currentRpm = 1000f
        isReverse = false
        isDrifting = false
        driftAngle = 0f
        driftComboTime = 0f
        driftScore = 0
        nitroFuel = 100f
        isNitroActive = false
        isAirborne = false
        airTime = 0f
    }

    /**
     * @param throttle Input 0.0 to 1.0 (Gas pedal)
     * @param brake Input 0.0 to 1.0 (Brake pedal)
     * @param steer Input -1.0 (Full Left) to +1.0 (Full Right)
     * @param handbrake Boolean (Drift button)
     * @param nitroRequested Boolean (NOS button)
     * @param dt Delta time in seconds (e.g. 0.016s)
     */
    fun update(
        throttle: Float,
        brake: Float,
        steer: Float,
        handbrake: Boolean,
        nitroRequested: Boolean,
        dt: Float,
        isOffRoad: Boolean = false
    ) {
        // Nitro logic
        if (nitroRequested && nitroFuel > 2f && speedKmH > 20f && !isReverse) {
            isNitroActive = true
            nitroFuel = (nitroFuel - 22f * dt).coerceAtLeast(0f)
        } else {
            isNitroActive = false
            // Slow passive refill when driving well
            if (speedKmH > 60f) {
                nitroFuel = (nitroFuel + 3.5f * dt).coerceAtMost(100f)
            }
        }

        val effectiveMaxSpeed = if (isNitroActive) maxSpeed * 1.25f else maxSpeed

        // Acceleration and braking
        val nitroBoostFactor = if (isNitroActive) 1.8f else 1.0f

        if (throttle > 0.05f && brake < 0.2f) {
            isBraking = false
            if (isReverse && speedKmH < 2f) {
                isReverse = false
            }

            if (!isReverse) {
                val accel = accelerationPower * throttle * 8.5f * nitroBoostFactor
                speedKmH += accel * dt
                if (speedKmH > effectiveMaxSpeed) speedKmH = effectiveMaxSpeed
            }
        } else if (brake > 0.2f && throttle < 0.1f) {
            // Braking or Reverse
            if (speedKmH > 3f) {
                isBraking = true
                val decel = brakePower * brake * 16.0f
                speedKmH = (speedKmH - decel * dt).coerceAtLeast(0f)
            } else {
                // In reverse
                isReverse = true
                isBraking = false
                speedKmH += 4.5f * brake * 5.0f * dt
                if (speedKmH > 50f) speedKmH = 50f // Max reverse speed
            }
        } else {
            isBraking = false
            // Natural drag / rolling friction
            val friction = if (isOffRoad) 28f else 6.5f
            speedKmH = (speedKmH - friction * dt).coerceAtLeast(0f)
        }

        // Off-road penalty
        if (isOffRoad && speedKmH > 75f) {
            speedKmH -= 35f * dt
        }

        // Steering dynamics
        val targetSteerAngle = steer * 32f
        steerVisualAngle += (targetSteerAngle - steerVisualAngle) * min(1f, 15f * dt)

        val speedFraction = (speedKmH / maxSpeed).coerceIn(0f, 1f)
        val turnAuthority = if (speedKmH > 5f) {
            // High speed turns require more stability
            (1.0f - speedFraction * 0.45f) * (handlingSens / 7f)
        } else {
            (speedKmH / 5f) * (handlingSens / 7f)
        }

        // Drift condition
        val driftInitiateSpeed = 45f
        val turningHard = abs(steer) > 0.4f
        val driftPressed = handbrake || (turningHard && speedKmH > driftInitiateSpeed && throttle > 0.7f)

        if (driftPressed && speedKmH > 35f && !isReverse) {
            isDrifting = true
            val targetDriftAngle = -steer * (38f + driftFactor * 2.5f)
            driftAngle += (targetDriftAngle - driftAngle) * min(1f, 8f * dt)
            // Extra nitro refill while drifting
            nitroFuel = (nitroFuel + 12f * dt).coerceAtMost(100f)

            // Drift score points
            driftComboTime += dt
            val pointsEarned = (abs(driftAngle) * (speedKmH / 30f) * dt * 8f).toInt()
            driftScore += pointsEarned
        } else {
            isDrifting = false
            driftAngle += (0f - driftAngle) * min(1f, 9f * dt)
            driftComboTime = (driftComboTime - dt * 2f).coerceAtLeast(0f)
        }

        // Heading angle update
        val dirSign = if (isReverse) -1f else 1f
        val yawDelta = steer * turnAuthority * 52f * dt * dirSign
        headingAngle += yawDelta

        // World displacement
        val speedMps = (speedKmH * 1000f) / 3600f
        val effectiveAngleRad = Math.toRadians((headingAngle + driftAngle * 0.4f).toDouble()).toFloat()

        posX += sin(effectiveAngleRad) * speedMps * dt * dirSign
        posZ += cos(effectiveAngleRad) * speedMps * dt * dirSign

        // Suspension pitch & roll
        val targetPitch = if (isBraking) 2.5f else if (throttle > 0.5f) -1.8f else 0f
        pitchAngle += (targetPitch - pitchAngle) * min(1f, 10f * dt)

        val targetRoll = -steer * (speedKmH / maxSpeed) * 4.5f
        rollAngle += (targetRoll - rollAngle) * min(1f, 12f * dt)

        // Wheel spin animation
        wheelSpinAngle = (wheelSpinAngle + speedMps * 25f * dt) % 360f

        // Gravity & Vertical physics (Airtime/Jumps)
        if (posY > 0.05f || velocityY > 0f) {
            isAirborne = true
            airTime += dt
            velocityY -= 22f * dt // Gravity
            posY += velocityY * dt
            if (posY <= 0f) {
                posY = 0f
                velocityY = 0f
                isAirborne = false
            }
        } else {
            isAirborne = false
            posY = 0f
        }

        // Transmission and RPM simulation
        updateTransmission(throttle)
    }

    private fun updateTransmission(throttle: Float) {
        if (isReverse) {
            currentGear = -1
            currentRpm = (1000f + (speedKmH / 50f) * 4500f).coerceIn(1000f, 6500f)
            return
        }

        // Automatic gear shifting
        var gear = 1
        for (g in 1..5) {
            if (speedKmH > gearMaxSpeeds[g] * 0.95f) {
                gear = g + 1
            }
        }
        currentGear = gear

        val minGearSpeed = if (currentGear == 1) 0f else gearMaxSpeeds[currentGear - 1] * 0.7f
        val maxGearSpeed = gearMaxSpeeds[currentGear]
        val ratio = ((speedKmH - minGearSpeed) / (maxGearSpeed - minGearSpeed + 0.1f)).coerceIn(0f, 1f)

        val targetRpmVal = 1200f + ratio * 5800f + throttle * 600f
        currentRpm += (targetRpmVal - currentRpm) * 0.2f
    }

    fun jump(force: Float = 10f) {
        if (!isAirborne) {
            velocityY = force
            isAirborne = true
        }
    }

    fun bounceFromBarrier(wallX: Float) {
        posX = wallX
        speedKmH *= 0.65f
        headingAngle = -headingAngle * 0.5f
    }
}
