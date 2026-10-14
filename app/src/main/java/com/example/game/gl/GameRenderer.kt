package com.example.game.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.CarPhysics
import com.example.game.engine.CharacterPhysics
import com.example.game.engine.TrackWorld
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*
import kotlin.random.Random

enum class CameraMode {
    CHASE,
    HOOD,
    ORBIT_GARAGE
}

class GameRenderer(
    private val getCarPhysics: () -> CarPhysics,
    private val getTrackWorld: () -> TrackWorld,
    private val getCarColorHex: () -> Long,
    private val getCharacterPhysics: () -> CharacterPhysics? = { null },
    private val getIsOnFoot: () -> Boolean = { false },
    private val isGarageMode: Boolean = false
) : GLSurfaceView.Renderer {

    private var program = 0
    private lateinit var carModel: Car3DModel
    private lateinit var characterModel: Character3DModel
    private lateinit var environment: Environment3D
    private lateinit var traffic3D: Traffic3D
    val particleSystem = ParticleSystem()

    var cameraMode = if (isGarageMode) CameraMode.ORBIT_GARAGE else CameraMode.CHASE
    var garageOrbitAngle = 45f

    // Camera state
    private var camPosX = 0f
    private var camPosY = 3.5f
    private var camPosZ = -8f
    private var camTargetX = 0f
    private var camTargetY = 1.0f
    private var camTargetZ = 5f

    private var shakeIntensity = 0f

    // Matrices
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    // Lighting & Fog
    private val lightPos = floatArrayOf(20f, 60f, 40f)
    private val viewPos = FloatArray(3)
    private val fogColor = floatArrayOf(0.07f, 0.08f, 0.12f) // Midnight Cyber Blue

    private var lastTimeMs = System.currentTimeMillis()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(fogColor[0], fogColor[1], fogColor[2], 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        program = ShaderUtil.createProgram(ShaderUtil.VERTEX_SHADER, ShaderUtil.FRAGMENT_SHADER)
        carModel = Car3DModel()
        characterModel = Character3DModel()
        environment = Environment3D()
        traffic3D = Traffic3D()

        lastTimeMs = System.currentTimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(1, height).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 60f, aspect, 0.5f, 350f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.currentTimeMillis()
        val dt = ((now - lastTimeMs) / 1000f).coerceIn(0.001f, 0.05f)
        lastTimeMs = now

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (program == 0) return

        GLES20.glUseProgram(program)

        // Set Fog parameters
        val uFogStartHandle = GLES20.glGetUniformLocation(program, "uFogStart")
        val uFogEndHandle = GLES20.glGetUniformLocation(program, "uFogEnd")
        val uFogColorHandle = GLES20.glGetUniformLocation(program, "uFogColor")
        GLES20.glUniform1f(uFogStartHandle, 40f)
        GLES20.glUniform1f(uFogEndHandle, 280f)
        GLES20.glUniform3fv(uFogColorHandle, 1, fogColor, 0)

        val car = getCarPhysics()
        val char = getCharacterPhysics()
        val isOnFoot = getIsOnFoot() && char != null
        val world = getTrackWorld()
        val carColor = getCarColorHex()

        // Update particles (only when driving)
        if (!isOnFoot && car.isDrifting && car.speedKmH > 30f) {
            val rearLeftX = car.posX - 0.9f
            val rearRightX = car.posX + 0.9f
            particleSystem.emitDriftSmoke(rearLeftX, car.posY, car.posZ - 1.4f)
            particleSystem.emitDriftSmoke(rearRightX, car.posY, car.posZ - 1.4f)
        }
        particleSystem.update(dt)

        // Dynamic Camera calculation
        if (isOnFoot && char != null) {
            updateCharacterCamera(char, dt)
        } else {
            updateCarCamera(car, dt)
        }

        viewPos[0] = camPosX
        viewPos[1] = camPosY
        viewPos[2] = camPosZ

        // Move light along with current active focal entity
        val focusX = if (isOnFoot && char != null) char.posX else car.posX
        val focusY = if (isOnFoot && char != null) char.posY else car.posY
        val focusZ = if (isOnFoot && char != null) char.posZ else car.posZ

        lightPos[0] = focusX + 15f
        lightPos[1] = focusY + 35f
        lightPos[2] = focusZ + 30f

        Matrix.setLookAtM(
            viewMatrix, 0,
            camPosX, camPosY, camPosZ,
            camTargetX, camTargetY, camTargetZ,
            0f, 1f, 0f
        )

        // View-Projection Matrix
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 1. Draw 3D Environment (Road, curbs, buildings, ramps, coins)
        if (!isGarageMode) {
            environment.draw(
                program, vpMatrix, lightPos, viewPos,
                focusZ, world.ramps, world.bonusCoins
            )

            // 2. Draw Traffic vehicles
            traffic3D.draw(
                program, vpMatrix, lightPos, viewPos,
                world.trafficCars, focusZ
            )
        } else {
            // Draw a stylish showroom floor in garage mode
            environment.draw(
                program, vpMatrix, lightPos, viewPos,
                0f, emptyList(), emptyList()
            )
        }

        // 3. Draw Player 3D Car
        val carDrawY = car.posY
        carModel.draw(
            program = program,
            vpMatrix = vpMatrix,
            lightPos = lightPos,
            viewPos = viewPos,
            posX = if (isGarageMode) 0f else car.posX,
            posY = if (isGarageMode) 0f else carDrawY,
            posZ = if (isGarageMode) 0f else car.posZ,
            headingAngle = if (isGarageMode) 0f else car.headingAngle,
            pitchAngle = if (isGarageMode) 0f else car.pitchAngle,
            rollAngle = if (isGarageMode) 0f else car.rollAngle,
            steerAngle = if (isGarageMode) 0f else car.steerVisualAngle,
            wheelSpinAngle = if (isGarageMode) 0f else car.wheelSpinAngle,
            carColorHex = carColor,
            isNitroActive = if (isOnFoot) false else car.isNitroActive,
            isBraking = if (isOnFoot) true else car.isBraking
        )

        // 4. Draw 3D Character when on foot
        if (isOnFoot && char != null && !isGarageMode) {
            characterModel.draw(
                program = program,
                vpMatrix = vpMatrix,
                lightPos = lightPos,
                viewPos = viewPos,
                posX = char.posX,
                posY = char.posY,
                posZ = char.posZ,
                headingAngle = char.headingAngle,
                walkPhase = char.walkPhase,
                isMoving = char.isMoving,
                isJumping = char.isJumping,
                suitColorHex = carColor
            )
        }

        // 5. Draw Particles (Smoke & Sparks)
        if (!isGarageMode) {
            particleSystem.draw(program, vpMatrix, lightPos, viewPos)
        }
    }

    private fun updateCharacterCamera(char: CharacterPhysics, dt: Float) {
        val headingRad = Math.toRadians(char.headingAngle.toDouble()).toFloat()
        val dist = 4.2f
        val height = 2.1f

        val idealCamX = char.posX - sin(headingRad) * dist
        val idealCamY = char.posY + height
        val idealCamZ = char.posZ - cos(headingRad) * dist

        val lerpFactor = min(1f, 10f * dt)
        camPosX += (idealCamX - camPosX) * lerpFactor
        camPosY += (idealCamY - camPosY) * lerpFactor
        camPosZ += (idealCamZ - camPosZ) * lerpFactor

        val idealTargetX = char.posX + sin(headingRad) * 1.5f
        val idealTargetY = char.posY + 1.1f
        val idealTargetZ = char.posZ + cos(headingRad) * 1.5f

        camTargetX += (idealTargetX - camTargetX) * lerpFactor
        camTargetY += (idealTargetY - camTargetY) * lerpFactor
        camTargetZ += (idealTargetZ - camTargetZ) * lerpFactor
    }

    private fun updateCarCamera(car: CarPhysics, dt: Float) {
        if (isGarageMode || cameraMode == CameraMode.ORBIT_GARAGE) {
            // Showroom 360 orbit
            val rad = Math.toRadians(garageOrbitAngle.toDouble()).toFloat()
            val dist = 6.2f
            camPosX = sin(rad) * dist
            camPosY = 2.0f
            camPosZ = cos(rad) * dist
            camTargetX = 0f
            camTargetY = 0.5f
            camTargetZ = 0f
            return
        }

        // Dynamic Shake
        if (shakeIntensity > 0f) {
            shakeIntensity = (shakeIntensity - 3f * dt).coerceAtLeast(0f)
        }
        val shakeX = if (shakeIntensity > 0f) (Random.nextFloat() - 0.5f) * shakeIntensity else 0f
        val shakeY = if (shakeIntensity > 0f) (Random.nextFloat() - 0.5f) * shakeIntensity else 0f

        when (cameraMode) {
            CameraMode.CHASE -> {
                // Smooth chase camera behind the car
                val headingRad = Math.toRadians(car.headingAngle.toDouble()).toFloat()
                val chaseDist = if (car.isNitroActive) 8.5f else 7.2f
                val chaseHeight = 2.6f + (car.speedKmH / 300f) * 0.8f

                val idealCamX = car.posX - sin(headingRad) * chaseDist + shakeX
                val idealCamY = car.posY + chaseHeight + shakeY
                val idealCamZ = car.posZ - cos(headingRad) * chaseDist

                // Smooth spring dampening
                val lerpFactor = min(1f, 9f * dt)
                camPosX += (idealCamX - camPosX) * lerpFactor
                camPosY += (idealCamY - camPosY) * lerpFactor
                camPosZ += (idealCamZ - camPosZ) * lerpFactor

                val idealTargetX = car.posX + sin(headingRad) * 6f
                val idealTargetY = car.posY + 1.2f
                val idealTargetZ = car.posZ + cos(headingRad) * 6f

                camTargetX += (idealTargetX - camTargetX) * lerpFactor
                camTargetY += (idealTargetY - camTargetY) * lerpFactor
                camTargetZ += (idealTargetZ - camTargetZ) * lerpFactor
            }
            CameraMode.HOOD -> {
                // Bumper / Hood camera
                val headingRad = Math.toRadians(car.headingAngle.toDouble()).toFloat()
                camPosX = car.posX + sin(headingRad) * 0.8f + shakeX
                camPosY = car.posY + 1.15f + shakeY
                camPosZ = car.posZ + cos(headingRad) * 0.8f

                camTargetX = car.posX + sin(headingRad) * 15f
                camTargetY = car.posY + 1.0f
                camTargetZ = car.posZ + cos(headingRad) * 15f
            }
            else -> {}
        }
    }

    fun triggerScreenShake(intensity: Float = 0.4f) {
        shakeIntensity = intensity
    }
}
