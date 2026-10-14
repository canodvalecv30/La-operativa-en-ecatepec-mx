package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.engine.BonusCoin
import com.example.game.engine.StuntRamp
import kotlin.math.sin

class Environment3D {

    // Road segments
    private val roadSegment = MeshBuilder.createBox(24f, 0.1f, 50f, 0.15f, 0.15f, 0.17f)
    private val laneLine = MeshBuilder.createBox(0.35f, 0.12f, 8f, 0.95f, 0.95f, 0.95f)
    private val curbRed = MeshBuilder.createBox(0.8f, 0.35f, 4f, 0.9f, 0.15f, 0.15f)
    private val curbWhite = MeshBuilder.createBox(0.8f, 0.35f, 4f, 0.95f, 0.95f, 0.95f)
    private val guardrail = MeshBuilder.createBox(0.4f, 0.8f, 50f, 0.7f, 0.7f, 0.75f)

    // Terrain & Buildings
    private val grassTerrain = MeshBuilder.createBox(120f, 0.05f, 50f, 0.12f, 0.40f, 0.18f)
    private val buildingTall = MeshBuilder.createBox(16f, 65f, 16f, 0.18f, 0.22f, 0.28f)
    private val buildingGlass = MeshBuilder.createBox(14f, 45f, 14f, 0.15f, 0.35f, 0.45f)
    private val treeTrunk = MeshBuilder.createCylinder(0.4f, 3.5f, 8, 0.4f, 0.25f, 0.15f)
    private val treeFoliage = MeshBuilder.createCylinder(2.2f, 3.5f, 8, 0.15f, 0.55f, 0.22f)

    // Stunt Ramp & Coin
    private val rampMesh = MeshBuilder.createRamp(6f, 10f, 3.5f, 0.95f, 0.5f, 0.05f)
    private val rampStripe = MeshBuilder.createBox(6.1f, 0.1f, 0.8f, 0.1f, 0.1f, 0.1f)
    private val coinMesh = MeshBuilder.createCylinder(0.8f, 0.22f, 12, 1.0f, 0.84f, 0.0f)

    // Scratch matrices
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun draw(
        program: Int,
        vpMatrix: FloatArray,
        lightPos: FloatArray,
        viewPos: FloatArray,
        playerZ: Float,
        ramps: List<StuntRamp>,
        coins: List<BonusCoin>
    ) {
        GLES20.glUseProgram(program)

        val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val uViewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val uOverrideColorHandle = GLES20.glGetUniformLocation(program, "uOverrideColor")

        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)

        // Draw rolling road tiles around player Z
        val segmentLength = 50f
        val centerSeg = (playerZ / segmentLength).toInt()

        for (s in (centerSeg - 2)..(centerSeg + 7)) {
            val segZ = s * segmentLength

            // Road surface
            drawMesh(roadSegment, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0f, segZ, 0.15f, 0.15f, 0.17f)

            // Grass terrain sides
            drawMesh(grassTerrain, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -72f, -0.05f, segZ, 0.12f, 0.40f, 0.18f)
            drawMesh(grassTerrain, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 72f, -0.05f, segZ, 0.12f, 0.40f, 0.18f)

            // Guardrails
            drawMesh(guardrail, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -12.3f, 0.4f, segZ, 0.7f, 0.7f, 0.75f)
            drawMesh(guardrail, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 12.3f, 0.4f, segZ, 0.7f, 0.7f, 0.75f)

            // Alternating Curbs along the segment
            for (c in 0 until 12) {
                val cz = segZ - 22f + c * 4f
                val curbColor = if (c % 2 == 0) curbRed else curbWhite
                val cr = if (c % 2 == 0) 0.95f else 0.95f
                val cg = if (c % 2 == 0) 0.15f else 0.95f
                val cb = if (c % 2 == 0) 0.15f else 0.95f
                drawMesh(curbColor, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -12.4f, 0.15f, cz, cr, cg, cb)
                drawMesh(curbColor, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 12.4f, 0.15f, cz, cr, cg, cb)
            }

            // White dotted lane markers
            for (laneX in floatArrayOf(-6f, 0f, 6f)) {
                for (d in 0 until 3) {
                    val lineZ = segZ - 16f + d * 16f
                    drawMesh(laneLine, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, laneX, 0.05f, lineZ, 0.95f, 0.95f, 0.95f)
                }
            }

            // City Skyscrapers on sides
            val bLeftType = if (s % 2 == 0) buildingTall else buildingGlass
            val bRightType = if (s % 3 == 0) buildingGlass else buildingTall
            val bLeftH = if (s % 2 == 0) 32f else 22f
            val bRightH = if (s % 3 == 0) 22f else 32f

            drawMesh(bLeftType, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -32f, bLeftH, segZ, 0.18f, 0.22f, 0.32f)
            drawMesh(bRightType, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 32f, bRightH, segZ + 12f, 0.20f, 0.30f, 0.40f)

            // Trees
            drawTree(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -17f, segZ - 10f)
            drawTree(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 17f, segZ + 10f)
        }

        // Draw Ramps
        for (ramp in ramps) {
            if (ramp.posZ in (playerZ - 40f)..(playerZ + 250f)) {
                drawMesh(rampMesh, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, ramp.posX, 0f, ramp.posZ, 0.95f, 0.55f, 0.05f)
                // Warning black stripes
                drawMesh(rampStripe, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, ramp.posX, 1.8f, ramp.posZ + 5f, 0.1f, 0.1f, 0.1f)
            }
        }

        // Draw Bonus Coins (Floating and spinning)
        val coinRot = (System.currentTimeMillis() % 3600) / 10f
        for (coin in coins) {
            if (!coin.collected && coin.posZ in (playerZ - 20f)..(playerZ + 200f)) {
                val bob = sin((System.currentTimeMillis() + coin.posZ * 20) * 0.005).toFloat() * 0.25f
                drawRotatingCoin(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, coin.posX, coin.posY + bob, coin.posZ, coinRot)
            }
        }
    }

    private fun drawTree(
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, tz: Float
    ) {
        // Trunk
        drawMesh(treeTrunk, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 1.75f, tz, 0.45f, 0.28f, 0.16f)
        // Foliage
        drawMesh(treeFoliage, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 4.5f, tz, 0.15f, 0.55f, 0.22f)
    }

    private fun drawRotatingCoin(
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, ty: Float, tz: Float,
        rotY: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(uColorHandle, 1.0f, 0.85f, 0.05f, 1f)

        renderMeshBuffer(coinMesh, program)
    }

    private fun drawMesh(
        mesh: MeshData,
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, ty: Float, tz: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, tz)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(uColorHandle, r, g, b, 1f)

        renderMeshBuffer(mesh, program)
    }

    private fun renderMeshBuffer(mesh: MeshData, program: Int) {
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
}
