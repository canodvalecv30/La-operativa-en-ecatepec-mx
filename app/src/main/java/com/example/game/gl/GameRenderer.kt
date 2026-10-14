package com.example.game.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.engine.CarPhysics
import com.example.game.engine.CharacterPhysics
import com.example.game.engine.GraphicsQuality
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
    private val getGraphicsQuality: () -> GraphicsQuality = { GraphicsQuality.HIGH },
    private val getCarDecal: () -> com.example.game.engine.DecalType = { com.example.game.engine.DecalType.NONE },
    private val getCarDecalColor: () -> Long = { 0xFFFFFFFF },
    private val getCarRimColor: () -> Long = { 0xFFE0E0E0 },
    private val getCarCaliperColor: () -> Long = { 0xFFE50914 },
    private val getCharacterSkin: () -> com.example.game.engine.CharacterSkin = { com.example.game.engine.CharacterSkin.PRO_RACER },
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
    var showCharacterInGarage = false

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

    private var viewportWidth = 1
    private var viewportHeight = 1
    private var viewportAspect = 1.77f
    private var currentFov = 60f

    // Lighting & Fog
    private val lightPos = floatArrayOf(20f, 60f, 40f)
    private val viewPos = FloatArray(3)
    private val fogColor = floatArrayOf(0.08f, 0.10f, 0.15f) // Midnight Cyber Blue atmosphere

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
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        viewportAspect = width.toFloat() / max(1, height).toFloat()
        Matrix.perspectiveM(projMatrix, 0, currentFov, viewportAspect, 0.5f, 350f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.currentTimeMillis()
        val dt = ((now - lastTimeMs) / 1000f).coerceIn(0.001f, 0.05f)
        lastTimeMs = now

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (program == 0) return

        GLES20.glUseProgram(program)

        val car = getCarPhysics()
        val char = getCharacterPhysics()
        val isOnFoot = getIsOnFoot() && char != null
        val world = getTrackWorld()
        val carColor = getCarColorHex()
        val quality = getGraphicsQuality()

        // Dynamic FOV Warp for Speed Sensation
        val speedRatio = (car.speedKmH / 300f).coerceIn(0f, 1f)
        val targetFov = if (isOnFoot) 58f else (58f + speedRatio * 12f + (if (car.isNitroActive) 8f else 0f))
        currentFov += (targetFov - currentFov) * min(1f, 8f * dt)
        Matrix.perspectiveM(projMatrix, 0, currentFov, viewportAspect, 0.5f, quality.renderDistance + 50f)

        // Set Fog parameters based on graphics quality
        val uFogStartHandle = GLES20.glGetUniformLocation(program, "uFogStart")
        val uFogEndHandle = GLES20.glGetUniformLocation(program, "uFogEnd")
        val uFogColorHandle = GLES20.glGetUniformLocation(program, "uFogColor")
        GLES20.glUniform1f(uFogStartHandle, 45f)
        GLES20.glUniform1f(uFogEndHandle, quality.renderDistance)
        GLES20.glUniform3fv(uFogColorHandle, 1, fogColor, 0)

        // Dynamic Headlight Projection
        val uHeadlightPosHandle = GLES20.glGetUniformLocation(program, "uHeadlightPos")
        val uHeadlightDirHandle = GLES20.glGetUniformLocation(program, "uHeadlightDir")
        val uEnableHeadlightHandle = GLES20.glGetUniformLocation(program, "uEnableHeadlight")

        val carHeadingRad = Math.toRadians(car.headingAngle.toDouble()).toFloat()
        val dirX = sin(carHeadingRad)
        val dirZ = cos(carHeadingRad)

        GLES20.glUniform3f(uHeadlightPosHandle, car.posX, car.posY + 0.45f, car.posZ + 1.2f)
        GLES20.glUniform3f(uHeadlightDirHandle, dirX, -0.06f, dirZ)
        GLES20.glUniform1i(uEnableHeadlightHandle, if (quality.shadowQuality >= 1 && !isOnFoot) 1 else 0)

        // Update particles
        if (!isOnFoot && car.isDrifting && car.speedKmH > 30f && quality.shadowQuality >= 1) {
            val rearLeftX = car.posX - 0.9f
            val rearRightX = car.posX + 0.9f
            particleSystem.emitDriftSmoke(rearLeftX, car.posY, car.posZ - 1.4f)
            particleSystem.emitDriftSmoke(rearRightX, car.posY, car.posZ - 1.4f)
        }
        particleSystem.update(dt)

        // Camera calculation
        if (isOnFoot && char != null) {
            updateCharacterCamera(char, dt)
        } else {
            updateCarCamera(car, dt)
        }

        viewPos[0] = camPosX
        viewPos[1] = camPosY
        viewPos[2] = camPosZ

        val focusX = if (isOnFoot && char != null) char.posX else car.posX
        val focusY = if (isOnFoot && char != null) char.posY else car.posY
        val focusZ = if (isOnFoot && char != null) char.posZ else car.posZ

        lightPos[0] = focusX + 20f
        lightPos[1] = focusY + 45f
        lightPos[2] = focusZ + 35f

        Matrix.setLookAtM(
            viewMatrix, 0,
            camPosX, camPosY, camPosZ,
            camTargetX, camTargetY, camTargetZ,
            0f, 1f, 0f
        )

        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 1. Draw 3D Environment with Distance Culling & Sun
        if (!isGarageMode) {
            environment.draw(
                program = program,
                vpMatrix = vpMatrix,
                lightPos = lightPos,
                viewPos = viewPos,
                playerZ = focusZ,
                ramps = world.ramps,
                coins = world.bonusCoins,
                renderDistance = quality.renderDistance,
                enableDetailedScenery = quality.shadowQuality >= 1
            )

            // 2. Draw Traffic vehicles
            traffic3D.draw(
                program, vpMatrix, lightPos, viewPos,
                world.trafficCars, focusZ
            )
        } else {
            environment.draw(
                program = program,
                vpMatrix = vpMatrix,
                lightPos = lightPos,
                viewPos = viewPos,
                playerZ = 0f,
                ramps = emptyList(),
                coins = emptyList(),
                renderDistance = 160f,
                enableDetailedScenery = false
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
            isBraking = if (isOnFoot) true else car.isBraking,
            decalType = getCarDecal(),
            decalColorHex = getCarDecalColor(),
            rimColorHex = getCarRimColor(),
            caliperColorHex = getCarCaliperColor()
        )

        // 4. Draw 3D Character when on foot or when toggled in Garage
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
                suitColorHex = carColor,
                skin = getCharacterSkin(),
                torsoPitch = char.torsoPitch,
                idlePhase = char.idlePhase,
                headTurn = char.headTurn
            )
        } else if (isGarageMode && showCharacterInGarage) {
            val idleTime = ((System.currentTimeMillis() % 100000) * 0.003f)
            characterModel.draw(
                program = program,
                vpMatrix = vpMatrix,
                lightPos = lightPos,
                viewPos = viewPos,
                posX = -1.85f,
                posY = 0f,
                posZ = 0.4f,
                headingAngle = 30f,
                walkPhase = 0f,
                isMoving = false,
                isJumping = false,
                suitColorHex = carColor,
                skin = getCharacterSkin(),
                torsoPitch = 0f,
                idlePhase = idleTime,
                headTurn = -15f
            )
        }

        // 5. Draw Particles
        if (!isGarageMode && quality.shadowQuality >= 1) {
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

        if (shakeIntensity > 0f) {
            shakeIntensity = (shakeIntensity - 3f * dt).coerceAtLeast(0f)
        }
        val shakeX = if (shakeIntensity > 0f) (Random.nextFloat() - 0.5f) * shakeIntensity else 0f
        val shakeY = if (shakeIntensity > 0f) (Random.nextFloat() - 0.5f) * shakeIntensity else 0f

        when (cameraMode) {
            CameraMode.CHASE -> {
                val headingRad = Math.toRadians(car.headingAngle.toDouble()).toFloat()
                val chaseDist = if (car.isNitroActive) 8.6f else 7.2f
                val chaseHeight = 2.6f + (car.speedKmH / 300f) * 0.8f

                val idealCamX = car.posX - sin(headingRad) * chaseDist + shakeX
                val idealCamY = car.posY + chaseHeight + shakeY
                val idealCamZ = car.posZ - cos(headingRad) * chaseDist

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
