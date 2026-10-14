package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.engine.DecalType

class Car3DModel {

    // Car Body Parts
    private val mainBody = MeshBuilder.createBox(2.05f, 0.65f, 4.3f, 0.9f, 0.1f, 0.1f)
    private val cabin = MeshBuilder.createBox(1.72f, 0.62f, 2.3f, 0.12f, 0.14f, 0.18f)
    private val windshieldSunstrip = MeshBuilder.createBox(1.68f, 0.10f, 0.05f, 0.08f, 0.08f, 0.09f)
    private val hoodScoop = MeshBuilder.createBox(0.95f, 0.10f, 1.3f, 0.10f, 0.10f, 0.12f)
    private val frontSplitter = MeshBuilder.createBox(2.15f, 0.10f, 0.85f, 0.08f, 0.08f, 0.09f)
    private val frontGrille = MeshBuilder.createBox(1.4f, 0.28f, 0.15f, 0.05f, 0.05f, 0.06f)
    private val rearDiffuser = MeshBuilder.createBox(2.15f, 0.20f, 0.65f, 0.08f, 0.08f, 0.09f)

    // Side Mirrors
    private val sideMirror = MeshBuilder.createBox(0.24f, 0.14f, 0.20f, 0.10f, 0.10f, 0.12f)
    private val mirrorGlass = MeshBuilder.createBox(0.04f, 0.11f, 0.16f, 0.70f, 0.80f, 0.90f)

    // Rear Spoiler
    private val spoilerWing = MeshBuilder.createBox(2.25f, 0.08f, 0.45f, 0.10f, 0.10f, 0.12f)
    private val spoilerStand = MeshBuilder.createBox(0.12f, 0.45f, 0.15f, 0.06f, 0.06f, 0.07f)

    // Lights
    private val headlight = MeshBuilder.createBox(0.42f, 0.16f, 0.12f, 0.85f, 0.95f, 1.0f)
    private val xenonLens = MeshBuilder.createBox(0.12f, 0.12f, 0.04f, 0.95f, 1.0f, 1.0f)
    private val taillight = MeshBuilder.createBox(0.58f, 0.14f, 0.10f, 1.0f, 0.05f, 0.05f)

    // Decals / Vinyl Meshes
    private val dualStripeHood = MeshBuilder.createBox(0.22f, 0.04f, 1.9f, 1f, 1f, 1f, 1f)
    private val dualStripeRoof = MeshBuilder.createBox(0.22f, 0.04f, 2.2f, 1f, 1f, 1f, 1f)
    private val sideSlash = MeshBuilder.createBox(0.04f, 0.16f, 2.1f, 1f, 1f, 1f, 1f)
    private val sideSlashDiagonal = MeshBuilder.createBox(0.04f, 0.14f, 1.4f, 1f, 1f, 1f, 1f)
    private val carbonHoodOverlay = MeshBuilder.createBox(1.90f, 0.04f, 1.85f, 0.10f, 0.10f, 0.12f, 1f)
    private val carbonRoofOverlay = MeshBuilder.createBox(1.65f, 0.04f, 2.15f, 0.10f, 0.10f, 0.12f, 1f)
    private val numberBadgeDisc = MeshBuilder.createCylinder(0.36f, 0.04f, 12, 1f, 1f, 1f, 1f)
    private val numberInnerDot = MeshBuilder.createBox(0.14f, 0.14f, 0.06f, 0.1f, 0.1f, 0.1f, 1f)

    // Exhaust & Nitro flame
    private val exhaustPipe = MeshBuilder.createCylinder(0.10f, 0.35f, 8, 0.75f, 0.75f, 0.80f)
    private val nitroFlame = MeshBuilder.createBox(0.20f, 0.20f, 1.5f, 0.1f, 0.85f, 1.0f)

    // Wheels (Tire + Rim + Brake Disc + Caliper)
    private val wheelTire = MeshBuilder.createCylinder(0.40f, 0.32f, 16, 0.14f, 0.14f, 0.14f)
    private val wheelRim = MeshBuilder.createCylinder(0.25f, 0.34f, 12, 0.88f, 0.88f, 0.92f)
    private val brakeRotor = MeshBuilder.createCylinder(0.22f, 0.04f, 10, 0.70f, 0.70f, 0.75f)
    private val brakeCaliper = MeshBuilder.createBox(0.08f, 0.14f, 0.14f, 0.95f, 0.05f, 0.05f)

    // Ground Contact Shadow
    private val contactShadow = MeshBuilder.createBox(2.35f, 0.02f, 4.4f, 0.03f, 0.03f, 0.04f, 0.65f)

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
        isBraking: Boolean,
        decalType: DecalType = DecalType.NONE,
        decalColorHex: Long = 0xFFFFFFFF,
        rimColorHex: Long = 0xFFE0E0E0,
        caliperColorHex: Long = 0xFFE50914
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

        // Decode decal color
        val dr = ((decalColorHex shr 16) and 0xFF) / 255.0f
        val dg = ((decalColorHex shr 8) and 0xFF) / 255.0f
        val db = (decalColorHex and 0xFF) / 255.0f

        // Decode rim color
        val rr = ((rimColorHex shr 16) and 0xFF) / 255.0f
        val rg = ((rimColorHex shr 8) and 0xFF) / 255.0f
        val rb = (rimColorHex and 0xFF) / 255.0f

        // Decode caliper color
        val calR = ((caliperColorHex shr 16) and 0xFF) / 255.0f
        val calG = ((caliperColorHex shr 8) and 0xFF) / 255.0f
        val calB = (caliperColorHex and 0xFF) / 255.0f

        // 0. Render Ground Contact Shadow
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, posX, 0.03f, posZ)
        Matrix.rotateM(modelMatrix, 0, headingAngle, 0f, 1f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        renderPart(contactShadow, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.02f, 0.02f, 0.03f, 0.65f)
        GLES20.glDisable(GLES20.GL_BLEND)

        // Base car world transformation
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, posX, posY + 0.45f, posZ)
        Matrix.rotateM(modelMatrix, 0, headingAngle, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, pitchAngle, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rollAngle, 0f, 0f, 1f)

        // 1. Main Chassis (Metallic gloss)
        renderPart(mainBody, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, cr, cg, cb, 1f)

        // 2. Decals / Vinyl liveries
        when (decalType) {
            DecalType.RACING_STRIPES -> {
                // Dual stripes on hood
                renderChild(dualStripeHood, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.32f, 0.35f, 1.25f, 0f, 0f, 0f, dr, dg, db, 1f)
                renderChild(dualStripeHood, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.32f, 0.35f, 1.25f, 0f, 0f, 0f, dr, dg, db, 1f)
                // Dual stripes on roof
                renderChild(dualStripeRoof, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.32f, 0.82f, -0.2f, 0f, 0f, 0f, dr, dg, db, 1f)
                renderChild(dualStripeRoof, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.32f, 0.82f, -0.2f, 0f, 0f, 0f, dr, dg, db, 1f)
            }
            DecalType.CYBER_SLASH -> {
                // Left door side slashes
                renderChild(sideSlash, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -1.04f, 0.05f, 0f, 0f, 0f, 0f, dr, dg, db, 1f)
                renderChild(sideSlashDiagonal, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -1.04f, 0.18f, 0.45f, 0f, 0f, 25f, dr, dg, db, 1f)
                // Right door side slashes
                renderChild(sideSlash, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 1.04f, 0.05f, 0f, 0f, 0f, 0f, dr, dg, db, 1f)
                renderChild(sideSlashDiagonal, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 1.04f, 0.18f, 0.45f, 0f, 0f, -25f, dr, dg, db, 1f)
            }
            DecalType.CARBON_ROOF -> {
                // Carbon fiber contrast overlays
                renderChild(carbonHoodOverlay, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.34f, 1.2f, 0f, 0f, 0f, 0.10f, 0.10f, 0.12f, 1f)
                renderChild(carbonRoofOverlay, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.82f, -0.2f, 0f, 0f, 0f, 0.10f, 0.10f, 0.12f, 1f)
            }
            DecalType.RACE_NUMBER -> {
                // Left door race number roundel
                renderChild(numberBadgeDisc, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -1.04f, 0.12f, 0f, 0f, 0f, 90f, dr, dg, db, 1f)
                renderChild(numberInnerDot, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -1.05f, 0.12f, 0f, 0f, 0f, 0f, 0.1f, 0.1f, 0.1f, 1f)
                // Right door race number roundel
                renderChild(numberBadgeDisc, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 1.04f, 0.12f, 0f, 0f, 0f, 90f, dr, dg, db, 1f)
                renderChild(numberInnerDot, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 1.05f, 0.12f, 0f, 0f, 0f, 0f, 0.1f, 0.1f, 0.1f, 1f)
            }
            else -> {}
        }

        // 3. Front Splitter & Grille
        renderChild(frontSplitter, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.25f, 2.15f, 0f, 0f, 0f, 0.08f, 0.08f, 0.09f, 1f)
        renderChild(frontGrille, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.05f, 2.18f, 0f, 0f, 0f, 0.05f, 0.05f, 0.06f, 1f)

        // 4. Rear Diffuser
        renderChild(rearDiffuser, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.22f, -2.15f, 0f, 0f, 0f, 0.08f, 0.08f, 0.09f, 1f)

        // 5. Cabin / Roof (Dark tinted aerodynamic glass)
        renderChild(cabin, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.50f, -0.2f, 0f, 0f, 0f, 0.12f, 0.14f, 0.18f, 1f)
        renderChild(windshieldSunstrip, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.75f, 0.90f, -25f, 0f, 0f, 0.08f, 0.08f, 0.09f, 1f)

        // 6. Hood scoop / vents
        renderChild(hoodScoop, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.35f, 1.25f, 0f, 0f, 0f, 0.10f, 0.10f, 0.12f, 1f)

        // 7. Side Rearview Mirrors
        renderChild(sideMirror, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -1.05f, 0.38f, 0.65f, 0f, -12f, 0f, cr * 0.8f, cg * 0.8f, cb * 0.8f, 1f)
        renderChild(mirrorGlass, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -1.06f, 0.38f, 0.62f, 0f, -12f, 0f, 0.70f, 0.80f, 0.90f, 1f)
        renderChild(sideMirror, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 1.05f, 0.38f, 0.65f, 0f, 12f, 0f, cr * 0.8f, cg * 0.8f, cb * 0.8f, 1f)
        renderChild(mirrorGlass, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 1.06f, 0.38f, 0.62f, 0f, 12f, 0f, 0.70f, 0.80f, 0.90f, 1f)

        // 8. Spoiler stands and wing
        renderChild(spoilerStand, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.7f, 0.45f, -1.95f, 0f, 0f, 0f, 0.08f, 0.08f, 0.08f, 1f)
        renderChild(spoilerStand, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.7f, 0.45f, -1.95f, 0f, 0f, 0f, 0.08f, 0.08f, 0.08f, 1f)
        renderChild(spoilerWing, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.72f, -1.95f, 8f, 0f, 0f, cr * 0.75f, cg * 0.75f, cb * 0.75f, 1f)

        // 9. Headlights with Xenon lenses
        renderChild(headlight, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.72f, 0.05f, 2.15f, 0f, 0f, 0f, 0.80f, 0.95f, 1.0f, 1f)
        renderChild(xenonLens, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.72f, 0.05f, 2.22f, 0f, 0f, 0f, 0.95f, 1.0f, 1.0f, 1f)
        renderChild(headlight, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.72f, 0.05f, 2.15f, 0f, 0f, 0f, 0.80f, 0.95f, 1.0f, 1f)
        renderChild(xenonLens, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.72f, 0.05f, 2.22f, 0f, 0f, 0f, 0.95f, 1.0f, 1.0f, 1f)

        // 10. Taillights
        val tailR = if (isBraking) 1.0f else 0.85f
        val tailG = if (isBraking) 0.35f else 0.05f
        renderChild(taillight, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.68f, 0.12f, -2.15f, 0f, 0f, 0f, tailR, tailG, 0.05f, 1f)
        renderChild(taillight, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.68f, 0.12f, -2.15f, 0f, 0f, 0f, tailR, tailG, 0.05f, 1f)

        // 11. Dual Exhausts
        renderChild(exhaustPipe, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.55f, -0.22f, -2.18f, 90f, 0f, 0f, 0.75f, 0.75f, 0.80f, 1f)
        renderChild(exhaustPipe, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.55f, -0.22f, -2.18f, 90f, 0f, 0f, 0.75f, 0.75f, 0.80f, 1f)

        // 12. Nitro Flames
        if (isNitroActive) {
            val flameFlicker = (System.currentTimeMillis() % 100) / 100f * 0.3f
            renderChild(nitroFlame, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.55f, -0.22f, -3.0f - flameFlicker, 0f, 0f, 0f, 0.1f, 0.85f, 1.0f, 0.95f)
            renderChild(nitroFlame, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.55f, -0.22f, -3.0f - flameFlicker, 0f, 0f, 0f, 0.1f, 0.85f, 1.0f, 0.95f)
        }

        // 13. Four 3D Wheels with custom Rim and Caliper colors
        val wheelOffsetX = 1.04f
        val wheelOffsetY = -0.15f
        val wheelFrontZ = 1.38f
        val wheelRearZ = -1.38f

        drawCustomWheel(program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -wheelOffsetX, wheelOffsetY, wheelFrontZ, steerAngle, wheelSpinAngle, isLeft = true, rr, rg, rb, calR, calG, calB)
        drawCustomWheel(program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, wheelOffsetX, wheelOffsetY, wheelFrontZ, steerAngle, wheelSpinAngle, isLeft = false, rr, rg, rb, calR, calG, calB)
        drawCustomWheel(program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -wheelOffsetX, wheelOffsetY, wheelRearZ, 0f, wheelSpinAngle, isLeft = true, rr, rg, rb, calR, calG, calB)
        drawCustomWheel(program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, wheelOffsetX, wheelOffsetY, wheelRearZ, 0f, wheelSpinAngle, isLeft = false, rr, rg, rb, calR, calG, calB)
    }

    private fun drawCustomWheel(
        program: Int,
        vpMatrix: FloatArray,
        carMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        ox: Float, oy: Float, oz: Float,
        steerAngle: Float,
        spinAngle: Float,
        isLeft: Boolean,
        rimR: Float, rimG: Float, rimB: Float,
        calR: Float, calG: Float, calB: Float
    ) {
        Matrix.multiplyMM(tempMatrix, 0, carMatrix, 0, createWheelLocalMatrix(ox, oy, oz, steerAngle, spinAngle), 0)
        renderPart(wheelTire, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.14f, 0.14f, 0.14f, 1f)
        renderPart(wheelRim, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, rimR, rimG, rimB, 1f)

        // Stationary Brake Rotor & Custom Caliper
        Matrix.multiplyMM(tempMatrix, 0, carMatrix, 0, createWheelLocalMatrix(ox, oy, oz, steerAngle, 0f), 0)
        renderPart(brakeRotor, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0.70f, 0.70f, 0.75f, 1f)
        renderChild(brakeCaliper, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, if (isLeft) -0.06f else 0.06f, 0.16f, 0f, 0f, 0f, 0f, calR, calG, calB, 1f)
    }

    private val wheelLocal = FloatArray(16)
    private fun createWheelLocalMatrix(ox: Float, oy: Float, oz: Float, steer: Float, spin: Float): FloatArray {
        Matrix.setIdentityM(wheelLocal, 0)
        Matrix.translateM(wheelLocal, 0, ox, oy, oz)
        Matrix.rotateM(wheelLocal, 0, steer, 0f, 1f, 0f)
        Matrix.rotateM(wheelLocal, 0, 90f, 0f, 0f, 1f)
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
