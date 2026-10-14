package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.engine.TrafficVehicle

class Traffic3D {

    // Sedan meshes
    private val sedanBody = MeshBuilder.createBox(2.0f, 0.70f, 4.3f, 0.2f, 0.4f, 0.8f)
    private val sedanCabin = MeshBuilder.createBox(1.7f, 0.65f, 2.3f, 0.12f, 0.15f, 0.18f)

    // SUV meshes
    private val suvBody = MeshBuilder.createBox(2.2f, 0.90f, 4.6f, 0.8f, 0.2f, 0.2f)
    private val suvCabin = MeshBuilder.createBox(1.9f, 0.75f, 3.2f, 0.15f, 0.15f, 0.15f)

    // Truck meshes
    private val truckCabin = MeshBuilder.createBox(2.4f, 1.6f, 2.4f, 0.9f, 0.8f, 0.1f)
    private val truckTrailer = MeshBuilder.createBox(2.5f, 2.0f, 6.5f, 0.85f, 0.85f, 0.88f)

    // Lights
    private val trafficHeadlight = MeshBuilder.createBox(0.35f, 0.15f, 0.10f, 0.95f, 0.95f, 0.85f)
    private val trafficTaillight = MeshBuilder.createBox(0.45f, 0.15f, 0.10f, 0.95f, 0.10f, 0.10f)

    // Wheels
    private val trafficWheel = MeshBuilder.createCylinder(0.38f, 0.30f, 10, 0.2f, 0.2f, 0.2f)

    // Scratch matrices
    private val modelMatrix = FloatArray(16)
    private val childMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun draw(
        program: Int,
        vpMatrix: FloatArray,
        lightPos: FloatArray,
        viewPos: FloatArray,
        trafficList: List<TrafficVehicle>,
        playerZ: Float
    ) {
        GLES20.glUseProgram(program)

        val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val uViewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val uOverrideColorHandle = GLES20.glGetUniformLocation(program, "uOverrideColor")

        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)

        for (v in trafficList) {
            if (v.posZ in (playerZ - 40f)..(playerZ + 250f)) {
                drawVehicle(v, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle)
            }
        }
    }

    private fun drawVehicle(
        v: TrafficVehicle,
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int
    ) {
        val cr = ((v.colorHex shr 16) and 0xFF) / 255.0f
        val cg = ((v.colorHex shr 8) and 0xFF) / 255.0f
        val cb = (v.colorHex and 0xFF) / 255.0f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, v.posX, 0.45f, v.posZ)

        if (v.isHit) {
            // Spin out when hit
            val spin = (v.hitTimer * 280f) % 360f
            Matrix.rotateM(modelMatrix, 0, spin, 0f, 1f, 0f)
        }

        when (v.vehicleType) {
            3 -> { // Truck
                // Cabin
                renderChild(truckCabin, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0.5f, 2.5f, 0f, 0f, 0f, cr, cg, cb)
                // Trailer
                renderChild(truckTrailer, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0.7f, -1.8f, 0f, 0f, 0f, 0.88f, 0.88f, 0.90f)
                // Headlights
                renderChild(trafficHeadlight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, -0.8f, 0.2f, 3.7f, 0f, 0f, 0f, 0.95f, 0.95f, 0.85f)
                renderChild(trafficHeadlight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.8f, 0.2f, 3.7f, 0f, 0f, 0f, 0.95f, 0.95f, 0.85f)
                // Taillights
                renderChild(trafficTaillight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, -0.9f, 0.2f, -5.0f, 0f, 0f, 0f, 0.95f, 0.1f, 0.1f)
                renderChild(trafficTaillight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.9f, 0.2f, -5.0f, 0f, 0f, 0f, 0.95f, 0.1f, 0.1f)
            }
            1 -> { // SUV
                renderChild(suvBody, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0f, 0f, 0f, 0f, 0f, cr, cg, cb)
                renderChild(suvCabin, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0.65f, -0.2f, 0f, 0f, 0f, 0.15f, 0.15f, 0.18f)
                renderChild(trafficHeadlight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, -0.75f, 0.1f, 2.3f, 0f, 0f, 0f, 0.95f, 0.95f, 0.85f)
                renderChild(trafficHeadlight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.75f, 0.1f, 2.3f, 0f, 0f, 0f, 0.95f, 0.95f, 0.85f)
                renderChild(trafficTaillight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, -0.75f, 0.1f, -2.3f, 0f, 0f, 0f, 0.95f, 0.1f, 0.1f)
                renderChild(trafficTaillight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.75f, 0.1f, -2.3f, 0f, 0f, 0f, 0.95f, 0.1f, 0.1f)
            }
            else -> { // Sedan / Sport
                renderChild(sedanBody, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0f, 0f, 0f, 0f, 0f, cr, cg, cb)
                renderChild(sedanCabin, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0.55f, -0.3f, 0f, 0f, 0f, 0.15f, 0.15f, 0.18f)
                renderChild(trafficHeadlight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, -0.70f, 0.05f, 2.15f, 0f, 0f, 0f, 0.95f, 0.95f, 0.85f)
                renderChild(trafficHeadlight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.70f, 0.05f, 2.15f, 0f, 0f, 0f, 0.95f, 0.95f, 0.85f)
                renderChild(trafficTaillight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, -0.70f, 0.05f, -2.15f, 0f, 0f, 0f, 0.95f, 0.1f, 0.1f)
                renderChild(trafficTaillight, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.70f, 0.05f, -2.15f, 0f, 0f, 0f, 0.95f, 0.1f, 0.1f)
            }
        }
    }

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
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(childMatrix, 0)
        Matrix.translateM(childMatrix, 0, tx, ty, tz)
        if (rx != 0f) Matrix.rotateM(childMatrix, 0, rx, 1f, 0f, 0f)
        if (ry != 0f) Matrix.rotateM(childMatrix, 0, ry, 0f, 1f, 0f)
        if (rz != 0f) Matrix.rotateM(childMatrix, 0, rz, 0f, 0f, 1f)
        Matrix.multiplyMM(tempMatrix, 0, parentMatrix, 0, childMatrix, 0)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, tempMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, tempMatrix, 0)
        GLES20.glUniform4f(uColorHandle, r, g, b, 1f)

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
