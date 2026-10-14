package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import kotlin.math.sin

class Character3DModel {

    // Head & Helmet
    private val helmet = MeshBuilder.createBox(0.40f, 0.40f, 0.42f, 0.95f, 0.95f, 0.98f)
    private val visor = MeshBuilder.createBox(0.38f, 0.16f, 0.12f, 0.10f, 0.85f, 1.00f) // Cyan reflective visor

    // Torso / Racing Suit
    private val torso = MeshBuilder.createBox(0.55f, 0.70f, 0.32f, 0.90f, 0.10f, 0.12f) // Racing red suit
    private val belt = MeshBuilder.createBox(0.56f, 0.10f, 0.33f, 0.15f, 0.15f, 0.18f)

    // Limbs (Arms & Legs)
    private val upperArm = MeshBuilder.createBox(0.16f, 0.36f, 0.16f, 0.85f, 0.10f, 0.12f)
    private val lowerArm = MeshBuilder.createBox(0.14f, 0.32f, 0.14f, 0.20f, 0.20f, 0.22f) // Black racing gloves

    private val upperLeg = MeshBuilder.createBox(0.20f, 0.42f, 0.20f, 0.18f, 0.20f, 0.25f) // Dark pants
    private val lowerLeg = MeshBuilder.createBox(0.18f, 0.38f, 0.18f, 0.90f, 0.10f, 0.12f) // Racing boots

    // Scratch matrices
    private val modelMatrix = FloatArray(16)
    private val limbMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun draw(
        program: Int,
        vpMatrix: FloatArray,
        lightPos: FloatArray,
        viewPos: FloatArray,
        posX: Float,
        posY: Float,
        posZ: Float,
        headingAngle: Float,
        walkPhase: Float, // 0..2PI
        isMoving: Boolean,
        isJumping: Boolean,
        suitColorHex: Long = 0xFFE50914
    ) {
        GLES20.glUseProgram(program)

        val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val uViewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val uOverrideColorHandle = GLES20.glGetUniformLocation(program, "uOverrideColor")

        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)

        // Suit primary color
        val sr = ((suitColorHex shr 16) and 0xFF) / 255.0f
        val sg = ((suitColorHex shr 8) and 0xFF) / 255.0f
        val sb = (suitColorHex and 0xFF) / 255.0f

        // Body bob while walking
        val bobY = if (isMoving && !isJumping) sin((walkPhase * 2.0).toDouble()).toFloat() * 0.06f else 0f

        // Base character position & rotation
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, posX, posY + 0.95f + bobY, posZ)
        Matrix.rotateM(modelMatrix, 0, headingAngle, 0f, 1f, 0f)

        // 1. Torso
        renderChild(torso, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.15f, 0f, 0f, 0f, 0f, sr, sg, sb, 1f)
        renderChild(belt, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.15f, 0f, 0f, 0f, 0f, 0.15f, 0.15f, 0.18f, 1f)

        // 2. Head & Visor
        renderChild(helmet, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.65f, 0f, 0f, 0f, 0f, 0.95f, 0.95f, 0.98f, 1f)
        renderChild(visor, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.65f, 0.20f, 0f, 0f, 0f, 0.10f, 0.85f, 1.0f, 1f)

        // Leg and arm swing calculation
        val limbSwing = if (isJumping) {
            30f // Spread legs in jump
        } else if (isMoving) {
            sin(walkPhase.toDouble()).toFloat() * 38f
        } else {
            0f
        }

        // 3. Left Leg (Swings forward/back)
        val leftLegAngle = if (isJumping) -20f else limbSwing
        drawLimb(upperLeg, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.16f, -0.38f, 0f, leftLegAngle, 0f, 0f, 0.2f, 0.22f, 0.28f)
        drawLimb(lowerLeg, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.16f, -0.72f, 0f, leftLegAngle * 0.8f, 0f, 0f, sr, sg, sb)

        // 4. Right Leg (Opposite swing)
        val rightLegAngle = if (isJumping) 25f else -limbSwing
        drawLimb(upperLeg, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.16f, -0.38f, 0f, rightLegAngle, 0f, 0f, 0.2f, 0.22f, 0.28f)
        drawLimb(lowerLeg, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.16f, -0.72f, 0f, rightLegAngle * 0.8f, 0f, 0f, sr, sg, sb)

        // 5. Left Arm (Swings opposite to left leg)
        val leftArmAngle = if (isJumping) -45f else -limbSwing * 0.85f
        drawLimb(upperArm, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.36f, 0.22f, 0f, leftArmAngle, 0f, 0f, sr, sg, sb)
        drawLimb(lowerArm, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.36f, -0.08f, 0f, leftArmAngle + 15f, 0f, 0f, 0.2f, 0.2f, 0.22f)

        // 6. Right Arm (Swings opposite to right leg)
        val rightArmAngle = if (isJumping) -45f else limbSwing * 0.85f
        drawLimb(upperArm, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.36f, 0.22f, 0f, rightArmAngle, 0f, 0f, sr, sg, sb)
        drawLimb(lowerArm, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.36f, -0.08f, 0f, rightArmAngle + 15f, 0f, 0f, 0.2f, 0.2f, 0.22f)
    }

    private fun drawLimb(
        mesh: MeshData,
        program: Int,
        vpMatrix: FloatArray,
        parentMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        ox: Float, oy: Float, oz: Float,
        rx: Float, ry: Float, rz: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(limbMatrix, 0)
        Matrix.translateM(limbMatrix, 0, ox, oy, oz)
        if (rx != 0f) Matrix.rotateM(limbMatrix, 0, rx, 1f, 0f, 0f)
        if (ry != 0f) Matrix.rotateM(limbMatrix, 0, ry, 0f, 1f, 0f)
        if (rz != 0f) Matrix.rotateM(limbMatrix, 0, rz, 0f, 0f, 1f)

        Matrix.multiplyMM(tempMatrix, 0, parentMatrix, 0, limbMatrix, 0)
        renderMeshDirect(mesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, r, g, b, 1f)
    }

    private val childMatrix = FloatArray(16)
    private fun renderChild(
        mesh: MeshData,
        program: Int,
        vpMatrix: FloatArray,
        parentMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, ty: Float, tz: Float,
        rx: Float, ry: Float, rz: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        Matrix.setIdentityM(childMatrix, 0)
        Matrix.translateM(childMatrix, 0, tx, ty, tz)
        if (rx != 0f) Matrix.rotateM(childMatrix, 0, rx, 1f, 0f, 0f)
        if (ry != 0f) Matrix.rotateM(childMatrix, 0, ry, 0f, 1f, 0f)
        if (rz != 0f) Matrix.rotateM(childMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.multiplyMM(tempMatrix, 0, parentMatrix, 0, childMatrix, 0)

        renderMeshDirect(mesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, r, g, b, a)
    }

    private fun renderMeshDirect(
        mesh: MeshData,
        program: Int,
        vpMatrix: FloatArray,
        mMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        r: Float, g: Float, b: Float, a: Float
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, mMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, mMatrix, 0)
        GLES20.glUniform4f(uColorHandle, r, g, b, a)

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
