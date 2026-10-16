package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.engine.Pedestrian
import com.example.game.engine.PedestrianState
import kotlin.math.cos
import kotlin.math.sin

class Pedestrian3D {

    // Head, Hair & Hats
    private val headMesh = MeshBuilder.createBox(0.24f, 0.26f, 0.24f, 0.92f, 0.76f, 0.65f)
    private val hairMesh = MeshBuilder.createBox(0.26f, 0.12f, 0.26f, 0.20f, 0.15f, 0.10f)
    private val capBrim = MeshBuilder.createBox(0.26f, 0.04f, 0.16f, 0.85f, 0.15f, 0.15f)
    private val sunglasses = MeshBuilder.createBox(0.24f, 0.06f, 0.05f, 0.10f, 0.10f, 0.12f)

    // Torso & Clothing
    private val torsoMesh = MeshBuilder.createBox(0.38f, 0.48f, 0.22f, 0.25f, 0.50f, 0.85f)
    private val jacketCollar = MeshBuilder.createBox(0.32f, 0.08f, 0.16f, 0.15f, 0.15f, 0.18f)
    private val beltMesh = MeshBuilder.createBox(0.36f, 0.06f, 0.21f, 0.15f, 0.15f, 0.15f)

    // Limbs - Arms & Hands
    private val armUpper = MeshBuilder.createBox(0.11f, 0.26f, 0.11f, 0.25f, 0.50f, 0.85f)
    private val armLower = MeshBuilder.createBox(0.09f, 0.24f, 0.09f, 0.92f, 0.76f, 0.65f)
    private val handMesh = MeshBuilder.createBox(0.08f, 0.10f, 0.08f, 0.92f, 0.76f, 0.65f)

    // Limbs - Legs & Shoes
    private val pelvisMesh = MeshBuilder.createBox(0.34f, 0.14f, 0.20f, 0.20f, 0.25f, 0.35f)
    private val legUpper = MeshBuilder.createBox(0.14f, 0.32f, 0.14f, 0.20f, 0.25f, 0.35f)
    private val legLower = MeshBuilder.createBox(0.12f, 0.30f, 0.12f, 0.20f, 0.25f, 0.35f)
    private val shoeMesh = MeshBuilder.createBox(0.13f, 0.08f, 0.22f, 0.90f, 0.90f, 0.92f)

    // Contact Shadow
    private val contactShadow = MeshBuilder.createBox(0.85f, 0.01f, 0.85f, 0.03f, 0.03f, 0.04f, 0.45f)

    // Scratch Matrices (Zero GC allocation during render frame)
    private val modelMatrix = FloatArray(16)
    private val limbMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun draw(
        program: Int,
        vpMatrix: FloatArray,
        lightPos: FloatArray,
        viewPos: FloatArray,
        pedestrians: List<Pedestrian>,
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

        for (ped in pedestrians) {
            val distZ = ped.posZ - playerZ
            if (distZ in -30f..renderDistance) {
                renderSinglePedestrian(
                    ped, program, vpMatrix,
                    uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle
                )
            }
        }
    }

    private fun renderSinglePedestrian(
        ped: Pedestrian,
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int
    ) {
        // Decode outfit color
        val cr = ((ped.outfitColorHex shr 16) and 0xFF) / 255.0f
        val cg = ((ped.outfitColorHex shr 8) and 0xFF) / 255.0f
        val cb = (ped.outfitColorHex and 0xFF) / 255.0f

        // Pants color
        val (pr, pg, pb) = when (ped.skinType) {
            0 -> Triple(0.18f, 0.20f, 0.25f) // Dark navy suit
            1 -> Triple(0.85f, 0.25f, 0.25f) // Sport shorts / red
            2 -> Triple(0.20f, 0.35f, 0.55f) // Classic denim jeans
            else -> Triple(0.88f, 0.82f, 0.70f) // Khaki beige
        }

        // Hair color
        val (hr, hg, hb) = when (ped.skinType) {
            0 -> Triple(0.12f, 0.12f, 0.14f) // Black hair
            1 -> Triple(0.75f, 0.65f, 0.35f) // Blonde hair
            2 -> Triple(0.35f, 0.22f, 0.12f) // Brunette hair
            else -> Triple(0.55f, 0.25f, 0.12f) // Auburn
        }

        // Walking animation phase
        val isDodging = ped.state == PedestrianState.DODGING
        val isWaving = ped.state == PedestrianState.WAVING
        val isMoving = ped.speed > 0.1f && !isDodging && !isWaving

        val walkBobY = if (isMoving) sin((ped.walkPhase * 2.0).toDouble()).toFloat() * 0.035f else 0f
        val legSwing = if (isMoving) sin(ped.walkPhase.toDouble()).toFloat() * 26f else 0f
        val armSwing = if (isMoving) sin(ped.walkPhase.toDouble()).toFloat() * 24f else 0f

        // Dodge evasion jump / leap
        val dodgeY = if (isDodging) (sin((ped.reactionTimer * 8f).toDouble()).toFloat() * 0.35f).coerceAtLeast(0f) else 0f

        // Ground Contact Shadow
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, ped.posX, 0.02f, ped.posZ)
        renderMeshDirect(contactShadow, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.03f, 0.03f, 0.04f, 0.45f)

        // Root Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, ped.posX, ped.posY + 0.85f + walkBobY + dodgeY, ped.posZ)
        Matrix.rotateM(modelMatrix, 0, ped.headingAngle, 0f, 1f, 0f)

        // 1. Torso & Clothes
        renderPart(torsoMesh, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0.15f, 0f, 0f, 0f, 0f, cr, cg, cb)
        renderPart(beltMesh, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.10f, 0f, 0f, 0f, 0f, 0.15f, 0.15f, 0.16f)

        // 2. Pelvis / Hips
        renderPart(pelvisMesh, program, vpMatrix, modelMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.18f, 0f, 0f, 0f, 0f, pr, pg, pb)

        // 3. Head & Hair (With Head Turn towards player or look ahead)
        Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, buildTranslation(0f, 0.52f, 0f), 0)
        if (ped.headTurnAngle != 0f) {
            Matrix.rotateM(tempMatrix, 0, ped.headTurnAngle, 0f, 1f, 0f)
        }
        renderDirect(headMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.92f, 0.76f, 0.65f)
        renderDirect(hairMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, hr, hg, hb)

        // Accessory: Sunglasses or Cap
        if (ped.skinType == 1 || ped.skinType == 3) {
            // Sunglasses
            renderPart(sunglasses, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0.02f, 0.13f, 0f, 0f, 0f, 0.10f, 0.10f, 0.12f)
        }

        // 4. Left Arm (Swings opposite to left leg)
        val leftArmAngle = when {
            isDodging -> 135f + sin((ped.reactionTimer * 12f).toDouble()).toFloat() * 15f
            else -> -armSwing
        }
        Matrix.multiplyMM(limbMatrix, 0, modelMatrix, 0, buildTranslation(-0.25f, 0.32f, 0f), 0)
        Matrix.rotateM(limbMatrix, 0, leftArmAngle, 1f, 0f, 0f)
        renderDirect(armUpper, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, cr, cg, cb)
        renderPart(armLower, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.22f, 0f, 0f, 0f, 0f, 0.92f, 0.76f, 0.65f)
        renderPart(handMesh, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.34f, 0f, 0f, 0f, 0f, 0.92f, 0.76f, 0.65f)

        // 5. Right Arm (Waving if player is on foot, or dodging, or normal swing)
        val rightArmAngle = when {
            isDodging -> 135f - sin((ped.reactionTimer * 12f).toDouble()).toFloat() * 15f
            isWaving -> 125f // Raised to wave
            else -> armSwing
        }
        Matrix.multiplyMM(limbMatrix, 0, modelMatrix, 0, buildTranslation(0.25f, 0.32f, 0f), 0)
        Matrix.rotateM(limbMatrix, 0, rightArmAngle, 1f, 0f, 0f)
        if (isWaving) {
            val waveHandZ = sin((System.currentTimeMillis() * 0.009f).toDouble()).toFloat() * 25f
            Matrix.rotateM(limbMatrix, 0, waveHandZ, 0f, 0f, 1f)
        }
        renderDirect(armUpper, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, cr, cg, cb)
        renderPart(armLower, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.22f, 0f, 0f, 0f, 0f, 0.92f, 0.76f, 0.65f)
        renderPart(handMesh, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.34f, 0f, 0f, 0f, 0f, 0.92f, 0.76f, 0.65f)

        // 6. Left Leg (Swings forward)
        val leftLegAngle = if (isDodging) -30f else legSwing
        Matrix.multiplyMM(limbMatrix, 0, modelMatrix, 0, buildTranslation(-0.11f, -0.26f, 0f), 0)
        Matrix.rotateM(limbMatrix, 0, leftLegAngle, 1f, 0f, 0f)
        renderDirect(legUpper, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, pr, pg, pb)
        renderPart(legLower, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.28f, 0f, 0f, 0f, 0f, pr, pg, pb)
        renderPart(shoeMesh, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.46f, 0.04f, 0f, 0f, 0f, 0.90f, 0.90f, 0.92f)

        // 7. Right Leg (Swings backward)
        val rightLegAngle = if (isDodging) 30f else -legSwing
        Matrix.multiplyMM(limbMatrix, 0, modelMatrix, 0, buildTranslation(0.11f, -0.26f, 0f), 0)
        Matrix.rotateM(limbMatrix, 0, rightLegAngle, 1f, 0f, 0f)
        renderDirect(legUpper, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, pr, pg, pb)
        renderPart(legLower, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.28f, 0f, 0f, 0f, 0f, pr, pg, pb)
        renderPart(shoeMesh, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.46f, 0.04f, 0f, 0f, 0f, 0.90f, 0.90f, 0.92f)
    }

    private fun renderPart(
        mesh: MeshData, program: Int, vpMatrix: FloatArray, parentMatrix: FloatArray,
        uMVPHandle: Int, uModelHandle: Int, uColorHandle: Int,
        tx: Float, ty: Float, tz: Float,
        rx: Float, ry: Float, rz: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, tx, ty, tz)
        if (rx != 0f) Matrix.rotateM(tempMatrix, 0, rx, 1f, 0f, 0f)
        if (ry != 0f) Matrix.rotateM(tempMatrix, 0, ry, 0f, 1f, 0f)
        if (rz != 0f) Matrix.rotateM(tempMatrix, 0, rz, 0f, 0f, 1f)

        Matrix.multiplyMM(limbMatrix, 0, parentMatrix, 0, tempMatrix, 0)
        renderDirect(mesh, program, vpMatrix, limbMatrix, uMVPHandle, uModelHandle, uColorHandle, r, g, b)
    }

    private fun renderDirect(
        mesh: MeshData, program: Int, vpMatrix: FloatArray, modelM: FloatArray,
        uMVPHandle: Int, uModelHandle: Int, uColorHandle: Int,
        r: Float, g: Float, b: Float
    ) {
        renderMeshDirect(mesh, program, vpMatrix, modelM, uMVPHandle, uModelHandle, uColorHandle, r, g, b, 1.0f)
    }

    private fun renderMeshDirect(
        mesh: MeshData, program: Int, vpMatrix: FloatArray, modelM: FloatArray,
        uMVPHandle: Int, uModelHandle: Int, uColorHandle: Int,
        r: Float, g: Float, b: Float, a: Float
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelM, 0)
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, modelM, 0)
        GLES20.glUniform4f(uColorHandle, r, g, b, a)

        val aPosHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val aNormHandle = GLES20.glGetAttribLocation(program, "aNormal")
        val aColorHandle = GLES20.glGetAttribLocation(program, "aColor")

        GLES20.glEnableVertexAttribArray(aPosHandle)
        GLES20.glEnableVertexAttribArray(aNormHandle)
        GLES20.glEnableVertexAttribArray(aColorHandle)

        mesh.vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPosHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.vertexBuffer)

        mesh.normalBuffer.position(0)
        GLES20.glVertexAttribPointer(aNormHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.normalBuffer)

        mesh.colorBuffer.position(0)
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, mesh.colorBuffer)

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES20.glDisableVertexAttribArray(aPosHandle)
        GLES20.glDisableVertexAttribArray(aNormHandle)
        GLES20.glDisableVertexAttribArray(aColorHandle)
    }

    private val translateScratch = FloatArray(16)
    private fun buildTranslation(tx: Float, ty: Float, tz: Float): FloatArray {
        Matrix.setIdentityM(translateScratch, 0)
        Matrix.translateM(translateScratch, 0, tx, ty, tz)
        return translateScratch
    }
}
