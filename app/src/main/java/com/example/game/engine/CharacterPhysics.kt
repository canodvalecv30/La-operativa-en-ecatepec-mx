package com.example.game.engine

import kotlin.math.*

class CharacterPhysics {
    var posX = 0f
    var posY = 0f
    var posZ = 0f

    var velocityY = 0f
    var headingAngle = 0f
    var walkPhase = 0f
    var idlePhase = 0f
    var torsoPitch = 0f
    var headTurn = 0f

    var isMoving = false
    var isSprinting = false
    var isJumping = false

    val walkSpeed = 5.5f // m/s
    val sprintSpeed = 11.5f // m/s
    val jumpForce = 8.5f // m/s

    fun spawnNearCar(car: CarPhysics) {
        // Step out to the left of the car (driver side)
        val carHeadingRad = Math.toRadians(car.headingAngle.toDouble()).toFloat()
        val doorOffsetX = -cos(carHeadingRad) * 2.2f
        val doorOffsetZ = sin(carHeadingRad) * 2.2f

        posX = car.posX + doorOffsetX
        posY = car.posY
        posZ = car.posZ + doorOffsetZ
        headingAngle = car.headingAngle
        velocityY = 0f
        walkPhase = 0f
        isMoving = false
        isSprinting = false
        isJumping = false
    }

    fun update(
        inputX: Float, // Left/Right: -1 to 1
        inputZ: Float, // Forward/Backward: -1 to 1
        sprint: Boolean,
        dt: Float
    ) {
        isSprinting = sprint
        val inputMag = sqrt(inputX * inputX + inputZ * inputZ)

        idlePhase = (idlePhase + 2.5f * dt) % (2.0 * Math.PI).toFloat()

        if (inputMag > 0.15f) {
            isMoving = true
            val speed = if (sprint) sprintSpeed else walkSpeed
            val normX = inputX / inputMag
            val normZ = inputZ / inputMag

            // Target heading from movement vector
            val targetAngle = Math.toDegrees(atan2(normX.toDouble(), normZ.toDouble())).toFloat()
            // Smoothly rotate character toward movement
            val angleDiff = (targetAngle - headingAngle)
            headingAngle += angleDiff * min(1f, 12f * dt)
            headTurn += ((angleDiff * 0.35f).coerceIn(-25f, 25f) - headTurn) * min(1f, 8f * dt)

            // Dynamic forward pitch when running / sprinting
            val targetPitch = if (sprint) 14f else 5f
            torsoPitch += (targetPitch - torsoPitch) * min(1f, 9f * dt)

            // Displace in world
            posX += normX * speed * dt
            posZ += normZ * speed * dt

            // Advance walk cycle animation
            val cadence = if (sprint) 18f else 10f
            walkPhase = (walkPhase + cadence * dt) % (2.0 * Math.PI).toFloat()
        } else {
            isMoving = false
            // Relax walk cycle and pitch towards 0
            walkPhase += (0f - walkPhase) * min(1f, 10f * dt)
            torsoPitch += (0f - torsoPitch) * min(1f, 8f * dt)
            headTurn += (0f - headTurn) * min(1f, 8f * dt)
        }

        // Gravity & Jump physics
        if (posY > 0.05f || velocityY > 0f) {
            isJumping = true
            velocityY -= 20f * dt // Gravity
            posY += velocityY * dt
            if (posY <= 0f) {
                posY = 0f
                velocityY = 0f
                isJumping = false
            }
        } else {
            isJumping = false
            posY = 0f
        }
    }

    fun jump() {
        if (!isJumping) {
            velocityY = jumpForce
            isJumping = true
        }
    }

    fun distanceToCar(car: CarPhysics): Float {
        val dx = posX - car.posX
        val dy = posY - car.posY
        val dz = posZ - car.posZ
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun canEnterCar(car: CarPhysics): Boolean {
        return distanceToCar(car) < 4.0f
    }
}
