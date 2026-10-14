package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix

class Car3DModel {

    // Car Body Parts
    private val mainBody = MeshBuilder.createBox(2.0f, 0.65f, 4.2f, 0.9f, 0.1f, 0.1f)
    private val cabin = MeshBuilder.createBox(1.7f, 0.60f, 2.2f, 0.15f, 0.15f, 0.18f)
    private val hoodScoop = MeshBuilder.createBox(0.9f, 0.12f, 1.2f, 0.1f, 0.1f, 0.1f)
    private val frontSplitter = MeshBuilder.createBox(2.1f, 0.10f, 0.8f, 0.08f, 0.08f, 0.08f)
    private val rearDiffuser = MeshBuilder.createBox(2.1f, 0.18f, 0.6f, 0.08f, 0.08f, 0.08f)

    // Rear Spoiler
    private val spoilerWing = MeshBuilder.createBox(2.2f, 0.08f, 0.45f, 0.1f, 0.1f, 0.12f)
    private val spoilerStand = MeshBuilder.createBox(0.12f, 0.45f, 0.15f, 0.05f, 0.05f, 0.05f)

    // Lights
    private val headlight = MeshBuilder.createBox(0.40f, 0.15f, 0.10f, 0.6f, 0.95f, 1.0f)
    private val taillight = MeshBuilder.createBox(0.55f, 0.12f, 0.10f, 1.0f, 0.05f, 0.05f)

    // Exhaust & Nitro flame
    private val exhaustPipe = MeshBuilder.createCylinder(0.10f, 0.35f, 8, 0.7f, 0.7f, 0.75f)
    private val nitroFlame = MeshBuilder.createBox(0.18f, 0.18f, 1.4f, 0.1f, 0.7f, 1.0f)

    // Wheels (Tire + Rim)
    private val wheelTire = MeshBuilder.createCylinder(0.40f, 0.32f, 16, 0.15f, 0.15f, 0.15f)
    private val wheelRim = MeshBuilder.createCylinder(0.24f, 0.34f, 12, 0.85f, 0.85f, 0.90f)

    // Scratch matrices
    private val modelMatrix = FloatArray(16)
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
        pitchAngle: Float,
        rollAngle: Float,
        steerAngle: Float,
        wheelSpinAngle: Float,
        carColorHex: Long,
        isNitroActive: Boolean,
        isBraking: Boolean
    ) {
        GLES20.glUseProgram(program)

        val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val uViewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val uOverrideColorHandle = GLES20.glGetUniformLocation(program, "uOverrideColor")

        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)

        // Decode car primary body color
        val cr = ((carColorHex shr 16) and 0xFF) / 255.0f
        val cg = ((carColorHex shr 8) and 0xFF) / 255.0f
        val cb = (carColorHex and 0xFF) / 255.0f

        // Base car world transformation
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, posX, posY + 0.45f, posZ)
        Matrix.rotateM(modelMatrix, 0, headingAngle, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, pitchAngle, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rollAngle, 0f, 0f, 1f)

        // 1. Draw Main Chassis (Tinted with car paint)
        renderPart(mainBody, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, cr, cg, cb, 1f)

        // 2. Front Splitter (Carbon fiber dark)
        renderChild(frontSplitter, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.25f, 2.1f, 0f, 0f, 0f, 0.1f, 0.1f, 0.12f, 1f)

        // 3. Rear Diffuser
        renderChild(rearDiffuser, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.22f, -2.1f, 0f, 0f, 0f, 0.1f, 0.1f, 0.12f, 1f)

        // 4. Cabin / Roof (Dark tinted glass + roof cap)
        renderChild(cabin, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.50f, -0.2f, 0f, 0f, 0f, 0.12f, 0.15f, 0.18f, 1f)

        // 5. Hood scoop / intake
        renderChild(hoodScoop, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.35f, 1.2f, 0f, 0f, 0f, 0.12f, 0.12f, 0.12f, 1f)

        // 6. Spoiler stands and wing
        renderChild(spoilerStand, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.7f, 0.45f, -1.9f, 0f, 0f, 0f, 0.1f, 0.1f, 0.1f, 1f)
        renderChild(spoilerStand, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.7f, 0.45f, -1.9f, 0f, 0f, 0f, 0.1f, 0.1f, 0.1f, 1f)
        renderChild(spoilerWing, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.70f, -1.9f, 8f, 0f, 0f, cr * 0.7f, cg * 0.7f, cb * 0.7f, 1f)

        // 7. Headlights (Bright xenon glow)
        renderChild(headlight, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.72f, 0.05f, 2.1f, 0f, 0f, 0f, 0.6f, 0.95f, 1.0f, 1f)
        renderChild(headlight, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.72f, 0.05f, 2.1f, 0f, 0f, 0f, 0.6f, 0.95f, 1.0f, 1f)

        // 8. Taillights (Neon red, brighter when braking)
        val tailR = if (isBraking) 1.0f else 0.8f
        val tailG = if (isBraking) 0.3f else 0.05f
        renderChild(taillight, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.68f, 0.12f, -2.1f, 0f, 0f, 0f, tailR, tailG, 0.05f, 1f)
        renderChild(taillight, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.68f, 0.12f, -2.1f, 0f, 0f, 0f, tailR, tailG, 0.05f, 1f)

        // 9. Dual Exhausts
        renderChild(exhaustPipe, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.55f, -0.22f, -2.15f, 90f, 0f, 0f, 0.7f, 0.7f, 0.75f, 1f)
        renderChild(exhaustPipe, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.55f, -0.22f, -2.15f, 90f, 0f, 0f, 0.7f, 0.7f, 0.75f, 1f)

        // 10. Nitro Flames when active
        if (isNitroActive) {
            val flameFlicker = (System.currentTimeMillis() % 100) / 100f * 0.3f
            renderChild(nitroFlame, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.55f, -0.22f, -2.9f - flameFlicker, 0f, 0f, 0f, 0.1f, 0.8f, 1.0f, 0.9f)
            renderChild(nitroFlame, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.55f, -0.22f, -2.9f - flameFlicker, 0f, 0f, 0f, 0.1f, 0.8f, 1.0f, 0.9f)
        }

        // 11. Four 3D Wheels
        val wheelOffsetX = 1.02f
        val wheelOffsetY = -0.15f
        val wheelFrontZ = 1.35f
        val wheelRearZ = -1.35f

        // Front-Left Wheel (Steering + Spin)
        drawWheel(program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -wheelOffsetX, wheelOffsetY, wheelFrontZ, steerAngle, wheelSpinAngle)
        // Front-Right Wheel (Steering + Spin)
        drawWheel(program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, wheelOffsetX, wheelOffsetY, wheelFrontZ, steerAngle, wheelSpinAngle)
        // Rear-Left Wheel (Spin only)
        drawWheel(program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -wheelOffsetX, wheelOffsetY, wheelRearZ, 0f, wheelSpinAngle)
        // Rear-Right Wheel (Spin only)
        drawWheel(program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, wheelOffsetX, wheelOffsetY, wheelRearZ, 0f, wheelSpinAngle)
    }

    private fun drawWheel(
        program: Int,
        vpMatrix: FloatArray,
        carMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        ox: Float, oy: Float, oz: Float,
        steerAngle: Float,
        spinAngle: Float
    ) {
        Matrix.multiplyMM(tempMatrix, 0, carMatrix, 0, createWheelLocalMatrix(ox, oy, oz, steerAngle, spinAngle), 0)
        // Draw Tire
        renderPart(wheelTire, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.18f, 0.18f, 0.18f, 1f)
        // Draw Rim
        renderPart(wheelRim, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.88f, 0.88f, 0.92f, 1f)
    }

    private val wheelLocal = FloatArray(16)
    private fun createWheelLocalMatrix(ox: Float, oy: Float, oz: Float, steer: Float, spin: Float): FloatArray {
        Matrix.setIdentityM(wheelLocal, 0)
        Matrix.translateM(wheelLocal, 0, ox, oy, oz)
        Matrix.rotateM(wheelLocal, 0, steer, 0f, 1f, 0f)
        Matrix.rotateM(wheelLocal, 0, 90f, 0f, 0f, 1f) // Align cylinder along X axis
        Matrix.rotateM(wheelLocal, 0, spin, 0f, 1f, 0f)
        return wheelLocal
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

        renderPart(mesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, r, g, b, a)
    }

    private fun renderPart(
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
