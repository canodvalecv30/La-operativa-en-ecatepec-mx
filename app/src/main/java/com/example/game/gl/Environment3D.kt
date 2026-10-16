package com.example.game.gl

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.engine.BonusCoin
import com.example.game.engine.CrossStreet
import com.example.game.engine.GameMap
import com.example.game.engine.StuntRamp
import kotlin.math.abs
import kotlin.math.sin

class Environment3D {

    // Main Road & Sidewalks
    private val roadSegment = MeshBuilder.createBox(24f, 0.1f, 50f, 0.16f, 0.16f, 0.18f)
    private val laneLine = MeshBuilder.createBox(0.35f, 0.12f, 8f, 0.95f, 0.95f, 0.95f)
    private val shoulderLine = MeshBuilder.createBox(0.40f, 0.12f, 50f, 0.95f, 0.80f, 0.10f)
    private val sidewalkSlab = MeshBuilder.createBox(4.5f, 0.25f, 50f, 0.42f, 0.44f, 0.48f)
    private val curbStone = MeshBuilder.createBox(0.4f, 0.28f, 50f, 0.75f, 0.75f, 0.80f)
    private val guardrail = MeshBuilder.createBox(0.35f, 0.85f, 50f, 0.72f, 0.72f, 0.78f)

    // Cross Streets / Intersections (Calles transversales)
    private val crossStreetAsphalt = MeshBuilder.createBox(130f, 0.09f, 22f, 0.15f, 0.15f, 0.17f)
    private val zebraCrosswalkStripe = MeshBuilder.createBox(0.9f, 0.13f, 4.5f, 0.96f, 0.96f, 0.96f)
    private val stopBar = MeshBuilder.createBox(22f, 0.13f, 0.6f, 0.96f, 0.96f, 0.96f)

    // Traffic Lights (Semáforos 3D)
    private val trafficLightMast = MeshBuilder.createCylinder(0.20f, 7.5f, 8, 0.25f, 0.26f, 0.30f)
    private val trafficLightArm = MeshBuilder.createBox(9.0f, 0.22f, 0.22f, 0.25f, 0.26f, 0.30f)
    private val trafficLightHousing = MeshBuilder.createBox(0.45f, 1.35f, 0.40f, 0.10f, 0.10f, 0.12f)
    private val lightBulbRed = MeshBuilder.createBox(0.22f, 0.22f, 0.10f, 1.0f, 0.15f, 0.15f)
    private val lightBulbYellow = MeshBuilder.createBox(0.22f, 0.22f, 0.10f, 1.0f, 0.85f, 0.10f)
    private val lightBulbGreen = MeshBuilder.createBox(0.22f, 0.22f, 0.10f, 0.15f, 0.95f, 0.35f)
    private val lightBulbOff = MeshBuilder.createBox(0.22f, 0.22f, 0.10f, 0.18f, 0.18f, 0.20f)

    // Street Signage & Urban Furniture
    private val streetSignPlate = MeshBuilder.createBox(3.4f, 0.55f, 0.10f, 0.10f, 0.45f, 0.85f) // Blue street name sign
    private val benchSeat = MeshBuilder.createBox(2.2f, 0.08f, 0.65f, 0.52f, 0.32f, 0.18f) // Wood bench
    private val benchLeg = MeshBuilder.createBox(0.12f, 0.45f, 0.60f, 0.15f, 0.15f, 0.18f)
    private val fireHydrant = MeshBuilder.createCylinder(0.22f, 0.75f, 8, 0.92f, 0.15f, 0.15f)
    private val busStopShelter = MeshBuilder.createBox(4.5f, 2.6f, 1.8f, 0.20f, 0.25f, 0.35f)
    private val busStopGlass = MeshBuilder.createBox(4.3f, 2.2f, 0.05f, 0.40f, 0.75f, 0.85f, 0.65f)

    // Street Lamps
    private val lampPost = MeshBuilder.createCylinder(0.25f, 8.5f, 8, 0.35f, 0.38f, 0.42f)
    private val lampArm = MeshBuilder.createBox(3.0f, 0.25f, 0.25f, 0.35f, 0.38f, 0.42f)
    private val lampHead = MeshBuilder.createBox(1.2f, 0.25f, 0.60f, 0.98f, 0.98f, 0.75f)

    // Neon City Props
    private val buildingTall = MeshBuilder.createBox(18f, 75f, 18f, 0.18f, 0.22f, 0.32f)
    private val buildingGlass = MeshBuilder.createBox(16f, 52f, 16f, 0.14f, 0.30f, 0.42f)
    private val neonBillboardCyan = MeshBuilder.createBox(10f, 4.5f, 0.4f, 0.0f, 0.90f, 1.0f)
    private val neonBillboardMagenta = MeshBuilder.createBox(10f, 4.5f, 0.4f, 0.95f, 0.10f, 0.75f)

    // Coastal Canyon Props (Mapa 2)
    private val oceanWater = MeshBuilder.createBox(160f, 0.05f, 50f, 0.05f, 0.45f, 0.75f)
    private val goldenBeach = MeshBuilder.createBox(25f, 0.08f, 50f, 0.92f, 0.82f, 0.55f)
    private val beachUmbrella = MeshBuilder.createCylinder(2.2f, 0.3f, 8, 0.95f, 0.35f, 0.15f)
    private val beachPole = MeshBuilder.createCylinder(0.08f, 2.2f, 6, 0.85f, 0.85f, 0.88f)
    private val canyonCliff = MeshBuilder.createBox(40f, 38f, 50f, 0.68f, 0.38f, 0.22f)
    private val palmTrunk = MeshBuilder.createCylinder(0.35f, 6.5f, 8, 0.48f, 0.32f, 0.18f)
    private val palmFronds = MeshBuilder.createBox(4.2f, 0.4f, 4.2f, 0.18f, 0.65f, 0.22f)

    // Stunt District Props (Mapa 3)
    private val containerRed = MeshBuilder.createBox(4.5f, 4.2f, 12f, 0.85f, 0.18f, 0.15f)
    private val containerBlue = MeshBuilder.createBox(4.5f, 4.2f, 12f, 0.15f, 0.35f, 0.85f)
    private val containerYellow = MeshBuilder.createBox(4.5f, 4.2f, 12f, 0.95f, 0.75f, 0.10f)
    private val containerGreen = MeshBuilder.createBox(4.5f, 4.2f, 12f, 0.15f, 0.65f, 0.35f)
    private val industrialCraneMast = MeshBuilder.createCylinder(0.7f, 28f, 8, 0.95f, 0.55f, 0.10f)
    private val industrialCraneArm = MeshBuilder.createBox(35f, 1.2f, 1.2f, 0.95f, 0.55f, 0.10f)

    // General Trees & Ramps & Coins
    private val treeTrunk = MeshBuilder.createCylinder(0.4f, 3.5f, 8, 0.42f, 0.26f, 0.16f)
    private val treeFoliage = MeshBuilder.createCylinder(2.2f, 3.5f, 8, 0.14f, 0.52f, 0.20f)
    private val rampMesh = MeshBuilder.createRamp(6f, 10f, 3.5f, 0.95f, 0.5f, 0.05f)
    private val rampStripe = MeshBuilder.createBox(6.1f, 0.1f, 0.8f, 0.1f, 0.1f, 0.1f)
    private val coinMesh = MeshBuilder.createCylinder(0.8f, 0.22f, 12, 1.0f, 0.84f, 0.0f)
    private val sunDisc = MeshBuilder.createCylinder(22f, 1.0f, 16, 1.0f, 0.95f, 0.70f)
    private val grassTerrain = MeshBuilder.createBox(120f, 0.05f, 50f, 0.12f, 0.38f, 0.18f)

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
        coins: List<BonusCoin>,
        crossStreets: List<CrossStreet> = emptyList(),
        map: GameMap = GameMap.NEON_CITY,
        renderDistance: Float = 260f,
        enableDetailedScenery: Boolean = true
    ) {
        GLES20.glUseProgram(program)

        val uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val uModelMatrixHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val uLightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val uViewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val uOverrideColorHandle = GLES20.glGetUniformLocation(program, "uOverrideColor")

        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)

        // Draw Sun Disc in the sky
        drawSun(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, playerZ, map)

        // Rolling road and scenery segments
        val segmentLength = 50f
        val centerSeg = (playerZ / segmentLength).toInt()
        val aheadSegments = (renderDistance / segmentLength).toInt().coerceIn(3, 10)

        val rc = map.roadColor
        val sc = map.shoulderColor

        for (s in (centerSeg - 2)..(centerSeg + aheadSegments)) {
            val segZ = s * segmentLength

            // 1. Road surface
            drawMesh(roadSegment, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 0f, 0f, segZ, rc[0], rc[1], rc[2])

            // 2. Road Shoulder Lines
            drawMesh(shoulderLine, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -11.6f, 0.06f, segZ, sc[0], sc[1], sc[2])
            drawMesh(shoulderLine, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 11.6f, 0.06f, segZ, sc[0], sc[1], sc[2])

            // 3. Elevated Concrete Sidewalks on both sides (Aceras Peatonales)
            drawMesh(sidewalkSlab, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -14.25f, 0.12f, segZ, 0.45f, 0.47f, 0.50f)
            drawMesh(sidewalkSlab, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 14.25f, 0.12f, segZ, 0.45f, 0.47f, 0.50f)

            // Raised Curb Stones (Bordillos de acera)
            drawMesh(curbStone, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -12.15f, 0.14f, segZ, 0.78f, 0.78f, 0.82f)
            drawMesh(curbStone, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 12.15f, 0.14f, segZ, 0.78f, 0.78f, 0.82f)

            // Outer Guardrails / Retaining barrier
            val barrierX = 16.7f
            drawMesh(guardrail, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -barrierX, 0.45f, segZ, 0.72f, 0.72f, 0.78f)
            drawMesh(guardrail, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, barrierX, 0.45f, segZ, 0.72f, 0.72f, 0.78f)

            // White dotted lane markers
            for (laneX in floatArrayOf(-6f, 0f, 6f)) {
                for (d in 0 until 3) {
                    val lineZ = segZ - 16f + d * 16f
                    drawMesh(laneLine, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, laneX, 0.05f, lineZ, 0.95f, 0.95f, 0.95f)
                }
            }

            // Street Furniture & Urban Details on Sidewalks
            if (enableDetailedScenery) {
                // Urban streetlamps every 50m
                drawStreetlight(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -15.8f, segZ, isLeft = true)
                drawStreetlight(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 15.8f, segZ, isLeft = false)

                // Park benches on sidewalks
                if (s % 2 == 0) {
                    drawBench(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -15.2f, segZ - 12f, rotY = 90f)
                    drawBench(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 15.2f, segZ + 12f, rotY = -90f)
                }

                // Fire hydrants
                if (s % 3 == 0) {
                    drawMesh(fireHydrant, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -15.5f, 0.55f, segZ + 5f, 0.95f, 0.15f, 0.15f)
                }

                // Transit bus stops
                if (s % 5 == 0) {
                    drawMesh(busStopShelter, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 15.2f, 1.35f, segZ - 6f, 0.20f, 0.25f, 0.35f)
                }
            }

            // 4. MAP SPECIFIC ENVIRONMENT
            when (map) {
                GameMap.NEON_CITY -> {
                    // Grass terrain behind sidewalks
                    drawMesh(grassTerrain, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -76f, -0.05f, segZ, 0.12f, 0.38f, 0.18f)
                    drawMesh(grassTerrain, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 76f, -0.05f, segZ, 0.12f, 0.38f, 0.18f)

                    // Towering Skyscrapers with neon accents
                    val bLeftH = if (s % 2 == 0) 38f else 26f
                    val bRightH = if (s % 3 == 0) 26f else 38f
                    drawMesh(buildingTall, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -36f, bLeftH, segZ, 0.18f, 0.22f, 0.32f)
                    drawMesh(buildingGlass, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 36f, bRightH, segZ + 12f, 0.16f, 0.32f, 0.44f)

                    // Neon Billboards on building facades
                    if (enableDetailedScenery) {
                        if (s % 2 == 0) {
                            drawMesh(neonBillboardCyan, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -26.5f, 12f, segZ, 0.0f, 0.92f, 1.0f)
                        } else {
                            drawMesh(neonBillboardMagenta, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 26.5f, 14f, segZ + 12f, 0.95f, 0.12f, 0.75f)
                        }
                        // Sidewalk trees
                        drawTree(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -15.5f, segZ + 18f)
                        drawTree(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 15.5f, segZ - 18f)
                    }
                }

                GameMap.COASTAL_SUNSET -> {
                    // Right Side: Golden Beach & Turquoise Ocean
                    drawMesh(goldenBeach, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 30f, 0.02f, segZ, 0.92f, 0.82f, 0.55f)
                    drawMesh(oceanWater, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 120f, -0.05f, segZ, 0.05f, 0.48f, 0.78f)

                    // Left Side: Sun-kissed Canyon Rock Cliffs & Villas
                    drawMesh(canyonCliff, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -42f, 19f, segZ, 0.68f, 0.38f, 0.22f)

                    // Palm trees along coastal promenade
                    if (enableDetailedScenery) {
                        drawPalmTree(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -15.5f, segZ + 10f)
                        drawPalmTree(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 15.5f, segZ - 10f)

                        // Beach umbrellas on sand
                        if (s % 2 == 0) {
                            drawBeachUmbrella(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 28f, segZ + 8f)
                        }
                    }
                }

                GameMap.STUNT_DISTRICT -> {
                    // Industrial concrete ground
                    drawMesh(grassTerrain, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -76f, -0.05f, segZ, 0.22f, 0.24f, 0.26f)
                    drawMesh(grassTerrain, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 76f, -0.05f, segZ, 0.22f, 0.24f, 0.26f)

                    // Shipping Containers stacked 1, 2, 3 high in industrial colors
                    val cLeft = if (s % 2 == 0) containerRed else containerBlue
                    val cRight = if (s % 2 == 0) containerYellow else containerGreen

                    drawMesh(cLeft, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -28f, 2.1f, segZ, 0.85f, 0.18f, 0.15f)
                    if (s % 2 == 0) {
                        // Stacked second level
                        drawMesh(containerBlue, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -28f, 6.3f, segZ, 0.15f, 0.35f, 0.85f)
                    }

                    drawMesh(cRight, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 28f, 2.1f, segZ + 10f, 0.95f, 0.75f, 0.10f)
                    if (s % 3 == 0) {
                        drawMesh(containerRed, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, 28f, 6.3f, segZ + 10f, 0.85f, 0.18f, 0.15f)
                    }

                    // Harbor Cranes
                    if (s % 4 == 0) {
                        drawMesh(industrialCraneMast, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -42f, 14f, segZ, 0.95f, 0.55f, 0.10f)
                        drawMesh(industrialCraneArm, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, -28f, 28f, segZ, 0.95f, 0.55f, 0.10f)
                    }
                }
            }
        }

        // 5. DRAW CROSS STREETS & INTERSECTIONS (MÁS CALLES)
        for (cross in crossStreets) {
            if (cross.posZ in (playerZ - 40f)..(playerZ + renderDistance)) {
                drawCrossStreet(
                    cross, program, vpMatrix,
                    uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle,
                    enableDetailedScenery
                )
            }
        }

        // 6. Draw Stunt Ramps
        for (ramp in ramps) {
            if (ramp.posZ in (playerZ - 40f)..(playerZ + renderDistance)) {
                val rampColor = if (map == GameMap.STUNT_DISTRICT) Triple(0.98f, 0.65f, 0.05f) else Triple(0.95f, 0.55f, 0.05f)
                drawMesh(rampMesh, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, ramp.posX, 0f, ramp.posZ, rampColor.first, rampColor.second, rampColor.third)
                drawMesh(rampStripe, program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, ramp.posX, 1.8f, ramp.posZ + 5f, 0.1f, 0.1f, 0.1f)
            }
        }

        // 7. Draw Bonus Coins (Floating and spinning)
        val coinRot = (System.currentTimeMillis() % 3600) / 10f
        for (coin in coins) {
            if (!coin.collected && coin.posZ in (playerZ - 20f)..(playerZ + renderDistance)) {
                val bob = sin((System.currentTimeMillis() + coin.posZ * 20) * 0.005).toFloat() * 0.25f
                drawRotatingCoin(program, vpMatrix, uMVPMatrixHandle, uModelMatrixHandle, uOverrideColorHandle, coin.posX, coin.posY + bob, coin.posZ, coinRot)
            }
        }
    }

    private fun drawCrossStreet(
        cross: CrossStreet,
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        detailed: Boolean
    ) {
        val cz = cross.posZ

        // 1. Cross-street perpendicular asphalt road
        drawMesh(crossStreetAsphalt, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0.02f, cz, 0.15f, 0.15f, 0.17f)

        // 2. Pedestrian Zebra Crosswalks (Pasos de Peatones con franjas blancas en ambos extremos del cruce)
        val crosswalkOffsetsZ = floatArrayOf(-12.5f, 12.5f)
        for (cwz in crosswalkOffsetsZ) {
            val zPos = cz + cwz
            // Stop bar
            drawMesh(stopBar, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, 0f, 0.06f, zPos + (if (cwz < 0) -2.6f else 2.6f), 0.95f, 0.95f, 0.95f)

            // 14 Zebra stripes across road width
            for (st in -6..6) {
                val sx = st * 1.8f
                drawMesh(zebraCrosswalkStripe, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, sx, 0.06f, zPos, 0.96f, 0.96f, 0.96f)
            }
        }

        // 3. 3D Traffic Light Gantries (Semáforos)
        if (cross.hasTrafficLight) {
            drawTrafficLightGantry(cross, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, -15.5f, cz - 13.5f, isLeft = true)
            drawTrafficLightGantry(cross, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, 15.5f, cz + 13.5f, isLeft = false)
        }

        // 4. Blue Street Name Sign ("Av. de la Libertad", "Gran Vía", etc.)
        if (detailed) {
            drawMesh(streetSignPlate, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, -14.2f, 4.6f, cz - 13.5f, 0.10f, 0.45f, 0.85f)
            drawMesh(streetSignPlate, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, 14.2f, 4.6f, cz + 13.5f, 0.10f, 0.45f, 0.85f)
        }
    }

    private fun drawTrafficLightGantry(
        cross: CrossStreet,
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float,
        tz: Float,
        isLeft: Boolean
    ) {
        // Vertical pole
        drawMesh(trafficLightMast, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 3.75f, tz, 0.25f, 0.26f, 0.30f)

        // Horizontal cantilever arm extending over traffic lanes
        val armOffset = if (isLeft) 4.5f else -4.5f
        drawMesh(trafficLightArm, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx + armOffset, 7.3f, tz, 0.25f, 0.26f, 0.30f)

        // Traffic Light housing boxes over left and center lanes
        for (boxX in floatArrayOf(tx + armOffset * 0.7f, tx + armOffset * 1.3f)) {
            drawMesh(trafficLightHousing, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, boxX, 6.2f, tz, 0.10f, 0.10f, 0.12f)

            // Red, Yellow, Green bulbs
            val bulbRed = if (cross.trafficLightState == 2) lightBulbRed else lightBulbOff
            val bulbYellow = if (cross.trafficLightState == 1) lightBulbYellow else lightBulbOff
            val bulbGreen = if (cross.trafficLightState == 0) lightBulbGreen else lightBulbOff

            val zOffset = if (isLeft) 0.21f else -0.21f
            drawMesh(bulbRed, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, boxX, 6.6f, tz + zOffset, bulbRed.colorBuffer.get(0), bulbRed.colorBuffer.get(1), bulbRed.colorBuffer.get(2))
            drawMesh(bulbYellow, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, boxX, 6.2f, tz + zOffset, bulbYellow.colorBuffer.get(0), bulbYellow.colorBuffer.get(1), bulbYellow.colorBuffer.get(2))
            drawMesh(bulbGreen, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, boxX, 5.8f, tz + zOffset, bulbGreen.colorBuffer.get(0), bulbGreen.colorBuffer.get(1), bulbGreen.colorBuffer.get(2))
        }
    }

    private fun drawPalmTree(
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, tz: Float
    ) {
        drawMesh(palmTrunk, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 3.25f, tz, 0.48f, 0.32f, 0.18f)
        drawMesh(palmFronds, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 6.5f, tz, 0.18f, 0.65f, 0.22f)
    }

    private fun drawBeachUmbrella(
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, tz: Float
    ) {
        drawMesh(beachPole, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 1.1f, tz, 0.85f, 0.85f, 0.88f)
        drawMesh(beachUmbrella, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 2.2f, tz, 0.95f, 0.35f, 0.15f)
    }

    private fun drawBench(
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, tz: Float, rotY: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, 0.5f, tz)
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)

        drawMesh(benchSeat, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 0.55f, tz, 0.52f, 0.32f, 0.18f)
        drawMesh(benchLeg, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx - 0.8f, 0.35f, tz, 0.15f, 0.15f, 0.18f)
        drawMesh(benchLeg, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx + 0.8f, 0.35f, tz, 0.15f, 0.15f, 0.18f)
    }

    private fun drawSun(
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        playerZ: Float,
        map: GameMap
    ) {
        val sc = map.sunColor
        val sunY = if (map == GameMap.COASTAL_SUNSET) 22f else 42f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 40f, sunY, playerZ + 220f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelHandle, 1, false, modelMatrix, 0)
        GLES20.glUniform4f(uColorHandle, sc[0], sc[1], sc[2], 1f)

        renderMeshBuffer(sunDisc, program)
    }

    private fun drawStreetlight(
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, tz: Float,
        isLeft: Boolean
    ) {
        drawMesh(lampPost, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 4.25f, tz, 0.35f, 0.38f, 0.42f)
        val armX = if (isLeft) tx + 1.2f else tx - 1.2f
        drawMesh(lampArm, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, armX, 8.4f, tz, 0.35f, 0.38f, 0.42f)
        val headX = if (isLeft) tx + 2.4f else tx - 2.4f
        drawMesh(lampHead, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, headX, 8.2f, tz, 0.98f, 0.98f, 0.75f)
    }

    private fun drawTree(
        program: Int,
        vpMatrix: FloatArray,
        uMVPHandle: Int,
        uModelHandle: Int,
        uColorHandle: Int,
        tx: Float, tz: Float
    ) {
        drawMesh(treeTrunk, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 1.75f, tz, 0.42f, 0.26f, 0.16f)
        drawMesh(treeFoliage, program, vpMatrix, uMVPHandle, uModelHandle, uColorHandle, tx, 4.5f, tz, 0.14f, 0.52f, 0.20f)
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
