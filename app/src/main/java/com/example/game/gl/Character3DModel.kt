package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.engine.CharacterSkin
import kotlin.math.cos
import kotlin.math.sin

class Character3DModel {

    // 1. Head & Helmet System (Aerodynamic motorsport helmet)
    private val helmetCrown = MeshBuilder.createBox(0.38f, 0.36f, 0.40f, 0.95f, 0.95f, 0.98f)
    private val helmetChin = MeshBuilder.createBox(0.32f, 0.16f, 0.24f, 0.15f, 0.15f, 0.18f)
    private val helmetSpoiler = MeshBuilder.createBox(0.26f, 0.08f, 0.16f, 0.10f, 0.10f, 0.12f)
    private val helmetVisor = MeshBuilder.createBox(0.34f, 0.14f, 0.12f, 0.10f, 0.85f, 1.00f) // Iridescent visor
    private val visorSunstrip = MeshBuilder.createBox(0.32f, 0.035f, 0.05f, 0.12f, 0.12f, 0.15f)
    private val visorPivotPod = MeshBuilder.createCylinder(0.045f, 0.04f, 8, 0.25f, 0.28f, 0.32f)

    // 2. Neck & Anatomical Torso
    private val neckCollar = MeshBuilder.createCylinder(0.12f, 0.12f, 8, 0.18f, 0.18f, 0.22f)
    private val chestPlate = MeshBuilder.createBox(0.54f, 0.38f, 0.30f, 0.90f, 0.10f, 0.12f) // V-taper chest
    private val chestStripe = MeshBuilder.createBox(0.14f, 0.38f, 0.04f, 0.95f, 0.95f, 0.98f) // Center racing stripe
    private val backAeroHump = MeshBuilder.createBox(0.24f, 0.34f, 0.15f, 0.12f, 0.12f, 0.15f) // Moto racing speed hump
    private val abdomenCore = MeshBuilder.createBox(0.46f, 0.26f, 0.26f, 0.82f, 0.10f, 0.12f)
    private val utilityBelt = MeshBuilder.createBox(0.48f, 0.08f, 0.28f, 0.15f, 0.16f, 0.18f)
    private val beltBuckle = MeshBuilder.createBox(0.12f, 0.08f, 0.04f, 0.85f, 0.85f, 0.90f) // Aluminum buckle
    private val beltPouch = MeshBuilder.createBox(0.08f, 0.10f, 0.08f, 0.12f, 0.12f, 0.14f)

    // 3. Arms, Deltoids & Ergonomic Racing Gloves
    private val shoulderArmor = MeshBuilder.createBox(0.18f, 0.16f, 0.18f, 0.15f, 0.15f, 0.18f)
    private val upperArmBicep = MeshBuilder.createBox(0.15f, 0.32f, 0.15f, 0.85f, 0.10f, 0.12f)
    private val elbowGuard = MeshBuilder.createBox(0.13f, 0.10f, 0.08f, 0.12f, 0.12f, 0.14f)
    private val forearmGauntlet = MeshBuilder.createBox(0.14f, 0.28f, 0.14f, 0.82f, 0.10f, 0.12f)
    private val gloveHand = MeshBuilder.createBox(0.12f, 0.16f, 0.10f, 0.18f, 0.18f, 0.20f)
    private val gloveKnuckles = MeshBuilder.createBox(0.13f, 0.04f, 0.06f, 0.85f, 0.85f, 0.90f)
    private val gloveThumb = MeshBuilder.createBox(0.05f, 0.09f, 0.06f, 0.18f, 0.18f, 0.20f)

    // 4. Pelvis, Legs & Performance Racing Boots
    private val pelvisHip = MeshBuilder.createBox(0.44f, 0.18f, 0.26f, 0.18f, 0.20f, 0.24f)
    private val upperLegThigh = MeshBuilder.createBox(0.20f, 0.42f, 0.21f, 0.18f, 0.20f, 0.24f)
    private val kneeSliderPuck = MeshBuilder.createBox(0.12f, 0.12f, 0.08f, 0.95f, 0.95f, 0.98f) // Knee slider
    private val lowerLegCalf = MeshBuilder.createBox(0.18f, 0.38f, 0.18f, 0.85f, 0.10f, 0.12f)
    private val bootUpper = MeshBuilder.createBox(0.16f, 0.16f, 0.26f, 0.14f, 0.14f, 0.16f)
    private val bootSole = MeshBuilder.createBox(0.17f, 0.05f, 0.32f, 0.08f, 0.08f, 0.09f) // Grippy rubber sole
    private val toeSlider = MeshBuilder.createBox(0.08f, 0.04f, 0.06f, 0.90f, 0.10f, 0.12f)

    // 5. Dynamic Ground Contact Shadow
    private val contactShadow = MeshBuilder.createBox(1.10f, 0.01f, 1.10f, 0.03f, 0.03f, 0.04f, 0.55f)

    // 6. Tactical Pistol & Cyber Weapon 3D Model
    private val gunSlide = MeshBuilder.createBox(0.09f, 0.10f, 0.32f, 0.18f, 0.20f, 0.24f)
    private val gunGrip = MeshBuilder.createBox(0.08f, 0.18f, 0.11f, 0.12f, 0.12f, 0.14f)
    private val gunBarrel = MeshBuilder.createCylinder(0.035f, 0.14f, 8, 0.10f, 0.10f, 0.12f)
    private val gunLaserSight = MeshBuilder.createBox(0.03f, 0.03f, 0.08f, 0.0f, 0.95f, 1.0f)

    // Scratch matrices for optimized performance (zero allocation during render)
    private val modelMatrix = FloatArray(16)
    private val limbMatrix = FloatArray(16)
    private val jointMatrix = FloatArray(16)
    private val childMatrix = FloatArray(16)
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
        walkPhase: Float,
        isMoving: Boolean,
        isJumping: Boolean,
        suitColorHex: Long = 0xFFE50914,
        skin: CharacterSkin = CharacterSkin.PRO_RACER,
        torsoPitch: Float = 0f,
        idlePhase: Float = 0f,
        headTurn: Float = 0f,
        isHoldingGun: Boolean = false,
        recoilKick: Float = 0f
    ) {
        GLES20.glUseProgram(program)

        val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val uViewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val uOverrideColorHandle = GLES20.glGetUniformLocation(program, "uOverrideColor")

        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)

        // Decode character skin colors
        val suitHex = if (skin == CharacterSkin.PRO_RACER) suitColorHex else skin.suitColorHex
        val sr = ((suitHex shr 16) and 0xFF) / 255.0f
        val sg = ((suitHex shr 8) and 0xFF) / 255.0f
        val sb = (suitHex and 0xFF) / 255.0f

        val ar = ((skin.accentColorHex shr 16) and 0xFF) / 255.0f
        val ag = ((skin.accentColorHex shr 8) and 0xFF) / 255.0f
        val ab = (skin.accentColorHex and 0xFF) / 255.0f

        val vr = ((skin.visorColorHex shr 16) and 0xFF) / 255.0f
        val vg = ((skin.visorColorHex shr 8) and 0xFF) / 255.0f
        val vb = (skin.visorColorHex and 0xFF) / 255.0f

        val hr = ((skin.helmetColorHex shr 16) and 0xFF) / 255.0f
        val hg = ((skin.helmetColorHex shr 8) and 0xFF) / 255.0f
        val hb = (skin.helmetColorHex and 0xFF) / 255.0f

        // Natural idle breathing cycle (chest expansion and micro-bob)
        val breathScale = if (!isMoving && !isJumping) sin(idlePhase.toDouble()).toFloat() * 0.018f else 0f
        val idleBobY = if (!isMoving && !isJumping) sin(idlePhase.toDouble()).toFloat() * 0.012f else 0f

        // Dynamic body bobbing while walking/sprinting
        val walkBobY = if (isMoving && !isJumping) sin((walkPhase * 2.0).toDouble()).toFloat() * 0.055f else 0f
        val lateralSway = if (isMoving && !isJumping) cos(walkPhase.toDouble()).toFloat() * 0.025f else 0f

        // Ground Contact Shadow directly under feet
        val shadowScale = if (isJumping) (1.0f - (posY / 5.0f)).coerceIn(0.5f, 1.0f) else 1.0f
        val shadowAlpha = if (isJumping) (0.55f - (posY / 6.0f)).coerceIn(0.15f, 0.55f) else 0.55f
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, posX, 0.015f, posZ)
        Matrix.scaleM(tempMatrix, 0, shadowScale, 1.0f, shadowScale)
        renderMeshDirect(contactShadow, program, vpMatrix, tempMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.02f, 0.02f, 0.03f, shadowAlpha)

        // Root Base Character Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, posX + lateralSway, posY + 0.95f + walkBobY + idleBobY, posZ)
        Matrix.rotateM(modelMatrix, 0, headingAngle, 0f, 1f, 0f)

        // Natural Forward Pitch (leans forward when running or sprinting)
        if (torsoPitch != 0f) {
            Matrix.rotateM(modelMatrix, 0, torsoPitch, 1f, 0f, 0f)
        }

        // 1. Pelvis / Hip Core
        renderChild(pelvisHip, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.22f, 0f, 0f, 0f, 0f, ar, ag, ab, 1f)

        // 2. Utility Belt with Aluminum Buckle & Telemetry Pouch
        renderChild(utilityBelt, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.13f, 0f, 0f, 0f, 0f, 0.15f, 0.16f, 0.18f, 1f)
        renderChild(beltBuckle, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.13f, 0.14f, 0f, 0f, 0f, 0.88f, 0.88f, 0.92f, 1f)
        renderChild(beltPouch, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.24f, -0.13f, 0f, 0f, 0f, 0f, 0.12f, 0.12f, 0.14f, 1f)

        // 3. Abdomen & V-Taper Muscular Chest
        renderChild(abdomenCore, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.01f, 0f, 0f, 0f, 0f, sr * 0.92f, sg * 0.92f, sb * 0.92f, 1f)
        renderChild(chestPlate, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.26f, 0f, 0f, 0f, 0f, sr, sg, sb, 1f)
        renderChild(chestStripe, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.26f, 0.14f, 0f, 0f, 0f, 0.95f, 0.95f, 0.98f, 1f)

        // Aerodynamic Speed Hump on Upper Back (MotoGP / Formula Racing Suit)
        renderChild(backAeroHump, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.26f, -0.18f, 12f, 0f, 0f, ar, ag, ab, 1f)

        // 4. Neck & High-Tech Racing Helmet
        renderChild(neckCollar, program, vpMatrix, modelMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.48f, 0f, 0f, 0f, 0f, 0.18f, 0.18f, 0.22f, 1f)

        // Head assembly matrix (with subtle lifelike look/turn)
        val headTurnAngle = headTurn + if (!isMoving && !isJumping) sin((idlePhase * 0.5).toDouble()).toFloat() * 4.0f else 0f
        Matrix.setIdentityM(jointMatrix, 0)
        Matrix.translateM(jointMatrix, 0, 0f, 0.70f, 0.02f)
        if (headTurnAngle != 0f) Matrix.rotateM(jointMatrix, 0, headTurnAngle, 0f, 1f, 0f)
        Matrix.multiplyMM(childMatrix, 0, modelMatrix, 0, jointMatrix, 0)

        // Helmet Crown & Chin Bar
        renderChildDirect(helmetCrown, program, vpMatrix, childMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0f, 0f, 0f, 0f, 0f, hr, hg, hb, 1f)
        renderChildDirect(helmetChin, program, vpMatrix, childMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.12f, 0.12f, 0f, 0f, 0f, ar, ag, ab, 1f)
        renderChildDirect(helmetSpoiler, program, vpMatrix, childMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.04f, -0.22f, -15f, 0f, 0f, ar, ag, ab, 1f)

        // Glossy Iridescent Reflective Visor & Sunstrip
        renderChildDirect(helmetVisor, program, vpMatrix, childMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, -0.01f, 0.18f, 0f, 0f, 0f, vr, vg, vb, 1f)
        renderChildDirect(visorSunstrip, program, vpMatrix, childMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0.07f, 0.20f, 0f, 0f, 0f, 0.12f, 0.12f, 0.15f, 1f)

        // Side Visor Pivot Pods
        renderChildDirect(visorPivotPod, program, vpMatrix, childMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -0.19f, -0.01f, 0.08f, 0f, 90f, 0f, 0.35f, 0.38f, 0.42f, 1f)
        renderChildDirect(visorPivotPod, program, vpMatrix, childMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0.19f, -0.01f, 0.08f, 0f, 90f, 0f, 0.35f, 0.38f, 0.42f, 1f)

        // Natural Biomechanical Limb Calculations
        val limbSwing = if (isJumping) {
            28f
        } else if (isMoving) {
            sin(walkPhase.toDouble()).toFloat() * 38f
        } else {
            0f
        }

        // ========================== LEGS & RACING BOOTS ==========================
        // 5. Left Leg (Thigh, Knee Slider, Calf, Racing Boot)
        val leftThighAngle = if (isJumping) -24f else limbSwing
        val leftKneeBend = if (isJumping) 35f else if (limbSwing < 0f) -limbSwing * 0.95f else 8f

        drawArticulatedLeg(
            upperMesh = upperLegThigh,
            kneeMesh = kneeSliderPuck,
            lowerMesh = lowerLegCalf,
            bootUpperMesh = bootUpper,
            bootSoleMesh = bootSole,
            toeSliderMesh = toeSlider,
            program = program,
            vpMatrix = vpMatrix,
            parentMatrix = modelMatrix,
            uMVPHandle = uMVPMatrixHandle,
            uModelHandle = uModelMatrixHandle,
            uColorHandle = uOverrideColorHandle,
            hipX = -0.16f, hipY = -0.32f, hipZ = 0f,
            thighAngle = leftThighAngle,
            kneeBendAngle = leftKneeBend,
            suitR = ar, suitG = ag, suitB = ab,
            accentR = sr, accentG = sg, accentB = sb
        )

        // 6. Right Leg (Opposite swing)
        val rightThighAngle = if (isJumping) -24f else -limbSwing
        val rightKneeBend = if (isJumping) 35f else if (-limbSwing < 0f) limbSwing * 0.95f else 8f

        drawArticulatedLeg(
            upperMesh = upperLegThigh,
            kneeMesh = kneeSliderPuck,
            lowerMesh = lowerLegCalf,
            bootUpperMesh = bootUpper,
            bootSoleMesh = bootSole,
            toeSliderMesh = toeSlider,
            program = program,
            vpMatrix = vpMatrix,
            parentMatrix = modelMatrix,
            uMVPHandle = uMVPMatrixHandle,
            uModelHandle = uModelMatrixHandle,
            uColorHandle = uOverrideColorHandle,
            hipX = 0.16f, hipY = -0.32f, hipZ = 0f,
            thighAngle = rightThighAngle,
            kneeBendAngle = rightKneeBend,
            suitR = ar, suitG = ag, suitB = ab,
            accentR = sr, accentG = sg, accentB = sb
        )

        // ========================== ARMS & RACING GLOVES ==========================
        // Arms swing opposite to legs with natural biomechanical elbow flexion
        val leftArmSwing = if (isJumping) -48f else if (isMoving) -limbSwing * 0.85f else 6f
        val leftElbowFlex = if (isJumping) 25f else if (isMoving) 55f + (-limbSwing * 0.35f).coerceAtLeast(0f) else 18f

        drawArticulatedArm(
            shoulderMesh = shoulderArmor,
            upperMesh = upperArmBicep,
            elbowMesh = elbowGuard,
            lowerMesh = forearmGauntlet,
            gloveMesh = gloveHand,
            knuckleMesh = gloveKnuckles,
            thumbMesh = gloveThumb,
            program = program,
            vpMatrix = vpMatrix,
            parentMatrix = modelMatrix,
            uMVPHandle = uMVPMatrixHandle,
            uModelHandle = uModelMatrixHandle,
            uColorHandle = uOverrideColorHandle,
            shoulderX = -0.36f, shoulderY = 0.36f, shoulderZ = 0f,
            armPitch = leftArmSwing,
            elbowBend = leftElbowFlex,
            armRoll = 8f,
            suitR = sr, suitG = sg, suitB = sb,
            armorR = ar, armorG = ag, armorB = ab,
            isLeft = true
        )

        val rightArmSwing = if (isHoldingGun) (-82f + recoilKick * 18f) else (if (isJumping) -48f else if (isMoving) limbSwing * 0.85f else 6f)
        val rightElbowFlex = if (isHoldingGun) 12f else (if (isJumping) 25f else if (isMoving) 55f + (limbSwing * 0.35f).coerceAtLeast(0f) else 18f)
        val rightArmRoll = if (isHoldingGun) -3f else -8f

        drawArticulatedArm(
            shoulderMesh = shoulderArmor,
            upperMesh = upperArmBicep,
            elbowMesh = elbowGuard,
            lowerMesh = forearmGauntlet,
            gloveMesh = gloveHand,
            knuckleMesh = gloveKnuckles,
            thumbMesh = gloveThumb,
            program = program,
            vpMatrix = vpMatrix,
            parentMatrix = modelMatrix,
            uMVPHandle = uMVPMatrixHandle,
            uModelHandle = uModelMatrixHandle,
            uColorHandle = uOverrideColorHandle,
            shoulderX = 0.36f, shoulderY = 0.36f, shoulderZ = 0f,
            armPitch = rightArmSwing,
            elbowBend = rightElbowFlex,
            armRoll = rightArmRoll,
            suitR = sr, suitG = sg, suitB = sb,
            armorR = ar, armorG = ag, armorB = ab,
            isLeft = false,
            isHoldingGun = isHoldingGun
        )
    }

    private fun drawArticulatedLeg(
        upperMesh: MeshData,
        kneeMesh: MeshData,
        lowerMesh: MeshData,
        bootUpperMesh: MeshData,
        bootSoleMesh: MeshData,
        toeSliderMesh: MeshData,
        program: Int,
        vpMatrix: FloatArray,
        parentMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        hipX: Float, hipY: Float, hipZ: Float,
        thighAngle: Float,
        kneeBendAngle: Float,
        suitR: Float, suitG: Float, suitB: Float,
        accentR: Float, accentG: Float, accentB: Float
    ) {
        // Hip joint rotation
        Matrix.setIdentityM(limbMatrix, 0)
        Matrix.translateM(limbMatrix, 0, hipX, hipY, hipZ)
        Matrix.rotateM(limbMatrix, 0, thighAngle, 1f, 0f, 0f)
        Matrix.multiplyMM(jointMatrix, 0, parentMatrix, 0, limbMatrix, 0)

        // Upper Thigh
        renderChildDirect(upperMesh, program, vpMatrix, jointMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.18f, 0f, 0f, 0f, 0f, suitR, suitG, suitB, 1f)

        // Knee joint & Knee Slider
        Matrix.setIdentityM(limbMatrix, 0)
        Matrix.translateM(limbMatrix, 0, 0f, -0.38f, 0f)
        Matrix.rotateM(limbMatrix, 0, kneeBendAngle, 1f, 0f, 0f)
        Matrix.multiplyMM(tempMatrix, 0, jointMatrix, 0, limbMatrix, 0)

        renderChildDirect(kneeMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0f, 0.12f, 0f, 0f, 0f, 0.95f, 0.95f, 0.98f, 1f)

        // Lower Leg (Shin/Calf)
        renderChildDirect(lowerMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.18f, 0f, 0f, 0f, 0f, accentR, accentG, accentB, 1f)

        // Racing Boot & Grippy Sole
        renderChildDirect(bootUpperMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.36f, 0.04f, 0f, 0f, 0f, 0.15f, 0.15f, 0.17f, 1f)
        renderChildDirect(bootSoleMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.44f, 0.05f, 0f, 0f, 0f, 0.08f, 0.08f, 0.09f, 1f)
        renderChildDirect(toeSliderMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, (if (hipX < 0) -0.09f else 0.09f), -0.42f, 0.18f, 0f, 0f, 0f, accentR, accentG, accentB, 1f)
    }

    private fun drawArticulatedArm(
        shoulderMesh: MeshData,
        upperMesh: MeshData,
        elbowMesh: MeshData,
        lowerMesh: MeshData,
        gloveMesh: MeshData,
        knuckleMesh: MeshData,
        thumbMesh: MeshData,
        program: Int,
        vpMatrix: FloatArray,
        parentMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        shoulderX: Float, shoulderY: Float, shoulderZ: Float,
        armPitch: Float,
        elbowBend: Float,
        armRoll: Float,
        suitR: Float, suitG: Float, suitB: Float,
        armorR: Float, armorG: Float, armorB: Float,
        isLeft: Boolean,
        isHoldingGun: Boolean = false
    ) {
        // Shoulder joint
        Matrix.setIdentityM(limbMatrix, 0)
        Matrix.translateM(limbMatrix, 0, shoulderX, shoulderY, shoulderZ)
        Matrix.rotateM(limbMatrix, 0, armPitch, 1f, 0f, 0f)
        Matrix.rotateM(limbMatrix, 0, armRoll, 0f, 0f, 1f)
        Matrix.multiplyMM(jointMatrix, 0, parentMatrix, 0, limbMatrix, 0)

        // Deltoid Armor Cap & Upper Arm
        renderChildDirect(shoulderMesh, program, vpMatrix, jointMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0f, 0f, 0f, 0f, 0f, armorR, armorG, armorB, 1f)
        renderChildDirect(upperMesh, program, vpMatrix, jointMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.16f, 0f, 0f, 0f, 0f, suitR, suitG, suitB, 1f)

        // Elbow Joint
        Matrix.setIdentityM(limbMatrix, 0)
        Matrix.translateM(limbMatrix, 0, 0f, -0.32f, 0f)
        Matrix.rotateM(limbMatrix, 0, elbowBend, 1f, 0f, 0f)
        Matrix.multiplyMM(tempMatrix, 0, jointMatrix, 0, limbMatrix, 0)

        renderChildDirect(elbowMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0f, -0.07f, 0f, 0f, 0f, 0.12f, 0.12f, 0.14f, 1f)

        // Forearm Gauntlet
        renderChildDirect(lowerMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.14f, 0f, 0f, 0f, 0f, suitR * 0.9f, suitG * 0.9f, suitB * 0.9f, 1f)

        // Racing Glove, Knuckle Protector, and Thumb
        renderChildDirect(gloveMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.30f, 0.01f, 0f, 0f, 0f, 0.18f, 0.18f, 0.20f, 1f)
        renderChildDirect(knuckleMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.28f, 0.06f, 0f, 0f, 0f, 0.88f, 0.88f, 0.92f, 1f)
        renderChildDirect(thumbMesh, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, (if (isLeft) 0.06f else -0.06f), -0.29f, 0.02f, 0f, 0f, 0f, 0.18f, 0.18f, 0.20f, 1f)

        // 3D Tactical Pistol firmly mounted in hand
        if (!isLeft && isHoldingGun) {
            renderChildDirect(gunGrip, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.38f, 0.05f, 15f, 0f, 0f, 0.12f, 0.12f, 0.14f, 1f)
            renderChildDirect(gunSlide, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.32f, 0.16f, 0f, 0f, 0f, 0.20f, 0.22f, 0.26f, 1f)
            renderChildDirect(gunBarrel, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.32f, 0.32f, 90f, 0f, 0f, 0.10f, 0.10f, 0.12f, 1f)
            renderChildDirect(gunLaserSight, program, vpMatrix, tempMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, -0.38f, 0.18f, 0f, 0f, 0f, 0.0f, 0.95f, 1.0f, 1f)
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

    private fun renderChildDirect(
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
        Matrix.multiplyMM(mvpMatrix, 0, parentMatrix, 0, childMatrix, 0)

        renderMeshDirect(mesh, program, vpMatrix, mvpMatrix, uMVPHandle, uModelHandle, uColorHandle, r, g, b, a)
    }

    private val directMvp = FloatArray(16)
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
        Matrix.multiplyMM(directMvp, 0, vpMatrix, 0, mMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, directMvp, 0)
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
