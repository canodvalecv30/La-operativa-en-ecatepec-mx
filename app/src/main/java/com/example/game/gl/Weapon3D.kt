package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.engine.Projectile
import com.example.game.engine.StuntCrate
import com.example.game.engine.TargetDrone

class Weapon3D {

    // Bullet / Laser Tracer Mesh
    private val bulletTracer = MeshBuilder.createBox(0.12f, 0.12f, 1.10f, 1.0f, 0.90f, 0.20f)
    private val bulletCore = MeshBuilder.createBox(0.08f, 0.08f, 0.70f, 1.0f, 1.0f, 1.0f)

    // Target Drone Meshes (Anti-gravity floating target)
    private val droneCore = MeshBuilder.createBox(0.65f, 0.65f, 0.65f, 0.20f, 0.22f, 0.26f)
    private val droneRing = MeshBuilder.createCylinder(0.95f, 0.15f, 12, 0.95f, 0.15f, 0.15f)
    private val droneBullseye = MeshBuilder.createCylinder(0.40f, 0.18f, 10, 1.0f, 0.95f, 0.20f)
    private val droneRotor = MeshBuilder.createBox(1.6f, 0.05f, 0.12f, 0.85f, 0.85f, 0.90f)

    // Stunt Crate Mesh (Explosive / Stunt destructible)
    private val crateBody = MeshBuilder.createBox(1.10f, 1.10f, 1.10f, 0.62f, 0.42f, 0.22f)
    private val crateBrace = MeshBuilder.createBox(1.15f, 0.15f, 1.15f, 0.25f, 0.25f, 0.28f)
    private val crateHazardStripe = MeshBuilder.createBox(0.90f, 0.45f, 0.04f, 0.95f, 0.80f, 0.10f)

    // Scratch matrices
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    fun draw(
        program: Int,
        vpMatrix: FloatArray,
        lightPos: FloatArray,
        viewPos: FloatArray,
        projectiles: List<Projectile>,
        drones: List<TargetDrone>,
        crates: List<StuntCrate>,
        playerZ: Float,
        renderDistance: Float = 220f
    ) {
        GLES20.glUseProgram(program)

        val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val uViewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val uOverrideColorHandle = GLES20.glGetUniformLocation(program, "uOverrideColor")

        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)

        // 1. Draw Flying Projectiles & Laser Tracers
        for (p in projectiles) {
            val distZ = p.posZ - playerZ
            if (distZ in -20f..renderDistance) {
                drawProjectile(p, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle)
            }
        }

        // 2. Draw Target Drones
        for (d in drones) {
            if (!d.isDestroyed && d.posZ in (playerZ - 20f)..(playerZ + renderDistance)) {
                drawTargetDrone(d, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle)
            }
        }

        // 3. Draw Stunt Crates
        for (c in crates) {
            if (!c.isDestroyed && c.posZ in (playerZ - 20f)..(playerZ + renderDistance)) {
                drawStuntCrate(c, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle)
            }
        }
    }

    private fun drawProjectile(
        p: Projectile,
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int
    ) {
        val hex = p.colorHex.toInt()
        val cr = ((hex shr 16) and 0xFF) / 255.0f
        val cg = ((hex shr 8) and 0xFF) / 255.0f
        val cb = (hex and 0xFF) / 255.0f

        val yawAngle = Math.toDegrees(kotlin.math.atan2(p.dirX.toDouble(), p.dirZ.toDouble())).toFloat()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, p.posX, p.posY, p.posZ)
        Matrix.rotateM(modelMatrix, 0, yawAngle, 0f, 1f, 0f)

        // Outer glow tracer
        drawMesh(bulletTracer, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, cr, cg, cb)
        // Bright white bullet core
        drawMesh(bulletCore, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 1.0f, 1.0f, 1.0f)
    }

    private fun drawTargetDrone(
        d: TargetDrone,
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, d.posX, d.posY, d.posZ)
        Matrix.rotateM(modelMatrix, 0, d.rotationAngle, 0f, 1f, 0f)

        // Metallic dark drone core
        drawMesh(droneCore, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.22f, 0.24f, 0.28f)

        // Red outer targeting ring
        Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, buildRotation(90f, 1f, 0f, 0f), 0)
        drawMesh(droneRing, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.95f, 0.15f, 0.15f)

        // Yellow bullseye core
        drawMesh(droneBullseye, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 1.0f, 0.85f, 0.05f)

        // Counter-rotating rotor blades
        Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, buildRotation(d.rotationAngle * 3f, 0f, 1f, 0f), 0)
        drawMesh(droneRotor, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.85f, 0.85f, 0.90f)
    }

    private fun drawStuntCrate(
        c: StuntCrate,
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, c.posX, c.posY, c.posZ)

        // Wood body
        drawMesh(crateBody, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.62f, 0.42f, 0.22f)
        // Reinforcing steel brackets
        drawMesh(crateBrace, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.25f, 0.25f, 0.28f)
        // Hazard warning stripe on front
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, 0f, 0f, 0.56f)
        Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, tempMatrix, 0)
        drawMesh(crateHazardStripe, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.95f, 0.80f, 0.10f)
    }

    private fun drawMesh(
        mesh: MeshData,
        program: Int,
        vpMatrix: FloatArray,
        modelM: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        r: Float, g: Float, b: Float
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelM, 0)
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, modelM, 0)
        GLES20.glUniform4f(uColorHandle, r, g, b, 1.0f)

        val aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val aNormalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        val aColorHandle = GLES20.glGetAttribLocation(program, "aColor")

        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mesh.vertexBuffer)

        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 12, mesh.normalBuffer)

        GLES20.glEnableVertexAttribArray(aColorHandle)
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 16, mesh.colorBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
        GLES20.glDisableVertexAttribArray(aColorHandle)
    }

    private val rotScratch = FloatArray(16)
    private fun buildRotation(angle: Float, ax: Float, ay: Float, az: Float): FloatArray {
        Matrix.setIdentityM(rotScratch, 0)
        Matrix.rotateM(rotScratch, 0, angle, ax, ay, az)
        return rotScratch
    }
}
