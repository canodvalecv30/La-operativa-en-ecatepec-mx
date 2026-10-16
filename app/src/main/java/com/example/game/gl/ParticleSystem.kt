package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import kotlin.random.Random

data class Particle(
    var posX: Float,
    var posY: Float,
    var posZ: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var size: Float,
    var r: Float,
    var g: Float,
    var b: Float,
    var alpha: Float,
    var life: Float,
    var maxLife: Float
)

class ParticleSystem {
    private val maxParticles = 120
    private val particles = ArrayList<Particle>(maxParticles)
    private val particleBox = MeshBuilder.createBox(0.25f, 0.25f, 0.25f, 1f, 1f, 1f, 1f)

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun emitDriftSmoke(x: Float, y: Float, z: Float) {
        if (particles.size >= maxParticles) return
        particles.add(
            Particle(
                posX = x + (Random.nextFloat() * 0.4f - 0.2f),
                posY = y + 0.1f,
                posZ = z + (Random.nextFloat() * 0.4f - 0.2f),
                vx = (Random.nextFloat() - 0.5f) * 1.5f,
                vy = Random.nextFloat() * 1.5f + 0.5f,
                vz = -Random.nextFloat() * 2f,
                size = 0.4f + Random.nextFloat() * 0.3f,
                r = 0.85f, g = 0.85f, b = 0.90f,
                alpha = 0.7f,
                life = 0f,
                maxLife = 0.5f
            )
        )
    }

    fun emitCrashSparks(x: Float, y: Float, z: Float) {
        for (i in 0 until 8) {
            if (particles.size >= maxParticles) break
            particles.add(
                Particle(
                    posX = x, posY = y, posZ = z,
                    vx = (Random.nextFloat() - 0.5f) * 10f,
                    vy = Random.nextFloat() * 8f + 2f,
                    vz = (Random.nextFloat() - 0.5f) * 10f,
                    size = 0.2f,
                    r = 1.0f, g = 0.7f, b = 0.1f,
                    alpha = 1.0f,
                    life = 0f,
                    maxLife = 0.35f
                )
            )
        }
    }

    fun emitMuzzleFlash(x: Float, y: Float, z: Float, r: Float = 1.0f, g: Float = 0.85f, b: Float = 0.2f) {
        for (i in 0 until 4) {
            if (particles.size >= maxParticles) break
            particles.add(
                Particle(
                    posX = x + (Random.nextFloat() - 0.5f) * 0.1f,
                    posY = y + (Random.nextFloat() - 0.5f) * 0.1f,
                    posZ = z + (Random.nextFloat() - 0.5f) * 0.1f,
                    vx = (Random.nextFloat() - 0.5f) * 2f,
                    vy = (Random.nextFloat() - 0.5f) * 2f,
                    vz = Random.nextFloat() * 4f + 2f,
                    size = 0.35f,
                    r = r, g = g, b = b,
                    alpha = 1.0f,
                    life = 0f,
                    maxLife = 0.12f
                )
            )
        }
    }

    fun emitBulletImpact(x: Float, y: Float, z: Float, r: Float = 1.0f, g: Float = 0.75f, b: Float = 0.2f) {
        for (i in 0 until 6) {
            if (particles.size >= maxParticles) break
            particles.add(
                Particle(
                    posX = x, posY = y, posZ = z,
                    vx = (Random.nextFloat() - 0.5f) * 8f,
                    vy = Random.nextFloat() * 6f + 1f,
                    vz = (Random.nextFloat() - 0.5f) * 8f,
                    size = 0.18f,
                    r = r, g = g, b = b,
                    alpha = 1.0f,
                    life = 0f,
                    maxLife = 0.25f
                )
            )
        }
    }

    fun emitTargetExplosion(x: Float, y: Float, z: Float) {
        for (i in 0 until 16) {
            if (particles.size >= maxParticles) break
            val isFire = i % 2 == 0
            particles.add(
                Particle(
                    posX = x, posY = y, posZ = z,
                    vx = (Random.nextFloat() - 0.5f) * 14f,
                    vy = Random.nextFloat() * 12f + 2f,
                    vz = (Random.nextFloat() - 0.5f) * 14f,
                    size = if (isFire) 0.45f else 0.25f,
                    r = if (isFire) 1.0f else 0.2f,
                    g = if (isFire) 0.55f else 0.85f,
                    b = if (isFire) 0.1f else 1.0f,
                    alpha = 1.0f,
                    life = 0f,
                    maxLife = 0.45f
                )
            )
        }
    }

    fun update(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life += dt
            if (p.life >= p.maxLife) {
                it.remove()
                continue
            }
            p.posX += p.vx * dt
            p.posY += p.vy * dt
            p.posZ += p.vz * dt
            p.size += 0.4f * dt
            p.alpha = (1.0f - p.life / p.maxLife).coerceIn(0f, 1f)
        }
    }

    fun draw(
        program: Int,
        vpMatrix: FloatArray,
        lightPos: FloatArray,
        viewPos: FloatArray
    ) {
        if (particles.isEmpty()) return

        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val uViewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val uOverrideColorHandle = GLES20.glGetUniformLocation(program, "uOverrideColor")

        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)

        for (p in particles) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.posX, p.posY, p.posZ)
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, p.size)

            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0)
            GLES20.glUniform4f(uOverrideColorHandle, p.r, p.g, p.b, p.alpha)

            val aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            val aNormalHandle = GLES20.glGetAttribLocation(program, "aNormal")
            val aColorHandle = GLES20.glGetAttribLocation(program, "aColor")

            GLES20.glEnableVertexAttribArray(aPositionHandle)
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 12, particleBox.vertexBuffer)

            GLES20.glEnableVertexAttribArray(aNormalHandle)
            GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 12, particleBox.normalBuffer)

            GLES20.glEnableVertexAttribArray(aColorHandle)
            GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 16, particleBox.colorBuffer)

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, particleBox.indexCount, GLES20.GL_UNSIGNED_SHORT, particleBox.indexBuffer)

            GLES20.glDisableVertexAttribArray(aPositionHandle)
            GLES20.glDisableVertexAttribArray(aNormalHandle)
            GLES20.glDisableVertexAttribArray(aColorHandle)
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }
}
