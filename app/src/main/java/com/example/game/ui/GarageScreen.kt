package com.example.game.ui

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.engine.*
import com.example.game.gl.CameraMode
import com.example.game.gl.GameRenderer

@Composable
fun GarageScreen(
    prefs: GamePreferences,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCarIndex by remember {
        mutableIntStateOf(
            CarCatalog.CARS.indexOfFirst { it.id == prefs.selectedCarId }.coerceAtLeast(0)
        )
    }

    val currentCarSpec = CarCatalog.CARS[selectedCarIndex]
    var currentCoins by remember { mutableIntStateOf(prefs.coins) }
    var carUnlocked by remember(selectedCarIndex) {
        mutableStateOf(prefs.isCarUnlocked(currentCarSpec.id))
    }

    var selectedColor by remember(selectedCarIndex) {
        mutableLongStateOf(prefs.getCarColor(currentCarSpec.id, currentCarSpec.defaultColorHex))
    }
    var selectedDecal by remember(selectedCarIndex) {
        mutableStateOf(prefs.getCarDecal(currentCarSpec.id))
    }
    var selectedDecalColor by remember(selectedCarIndex) {
        mutableLongStateOf(prefs.getCarDecalColor(currentCarSpec.id))
    }
    var selectedRimColor by remember(selectedCarIndex) {
        mutableLongStateOf(prefs.getCarRimColor(currentCarSpec.id))
    }
    var selectedCaliperColor by remember(selectedCarIndex) {
        mutableLongStateOf(prefs.getCarCaliperColor(currentCarSpec.id))
    }

    var engineLevel by remember(selectedCarIndex) {
        mutableIntStateOf(prefs.getUpgradeLevel(currentCarSpec.id, "engine"))
    }
    var handlingLevel by remember(selectedCarIndex) {
        mutableIntStateOf(prefs.getUpgradeLevel(currentCarSpec.id, "handling"))
    }
    var turboLevel by remember(selectedCarIndex) {
        mutableIntStateOf(prefs.getUpgradeLevel(currentCarSpec.id, "turbo"))
    }
    var brakesLevel by remember(selectedCarIndex) {
        mutableIntStateOf(prefs.getUpgradeLevel(currentCarSpec.id, "brakes"))
    }

    // 3D Preview Physics and Renderer
    val dummyPhysics = remember(selectedCarIndex) {
        CarPhysics(currentCarSpec).apply {
            posX = 0f
            posY = 0f
            posZ = 0f
            headingAngle = 0f
        }
    }
    val dummyWorld = remember { TrackWorld(GameMode.FREE_ROAM) }

    var showPilotInGarage by remember { mutableStateOf(false) }

    val garageRenderer = remember(selectedCarIndex) {
        GameRenderer(
            getCarPhysics = { dummyPhysics },
            getTrackWorld = { dummyWorld },
            getCarColorHex = { selectedColor },
            getCarDecal = { selectedDecal },
            getCarDecalColor = { selectedDecalColor },
            getCarRimColor = { selectedRimColor },
            getCarCaliperColor = { selectedCaliperColor },
            getCharacterSkin = { prefs.characterSkin },
            isGarageMode = true
        ).apply {
            cameraMode = CameraMode.ORBIT_GARAGE
            garageOrbitAngle = 40f
            showCharacterInGarage = showPilotInGarage
        }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Info/Stats, 1 = Personalizar (Colores & Decals), 2 = Mejoras

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1118))
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2230))
                        .testTag("garage_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "GARAJE Y PERSONALIZACIÓN",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            // Coins balance pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E2230),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Monedas",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$currentCoins",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 2-COLUMN LANDSCAPE LAYOUT
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT COLUMN: 3D Showroom + Car Selector + Buy Button
            Column(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3D SHOWROOM VIEWPORT (Draggable 360°)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF07090E))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                garageRenderer.garageOrbitAngle += dragAmount.x * 0.5f
                            }
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            GLSurfaceView(ctx).apply {
                                setEGLContextClientVersion(2)
                                setRenderer(garageRenderer)
                                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x99000000)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gira 360°", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (showPilotInGarage) Color(0xFF00E5FF) else Color(0x99000000),
                            modifier = Modifier
                                .clickable {
                                    showPilotInGarage = !showPilotInGarage
                                    garageRenderer.showCharacterInGarage = showPilotInGarage
                                }
                                .testTag("garage_toggle_pilot_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Piloto 3D",
                                    tint = if (showPilotInGarage) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (showPilotInGarage) "Piloto Activo" else "Ver Piloto 3D",
                                    color = if (showPilotInGarage) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // CAR SELECTOR ROW
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CarCatalog.CARS.indices.toList()) { index ->
                        val car = CarCatalog.CARS[index]
                        val isSelected = index == selectedCarIndex
                        val isUnlocked = prefs.isCarUnlocked(car.id)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFE50914) else Color(0xFF1E2230),
                            modifier = Modifier
                                .clickable {
                                    selectedCarIndex = index
                                    if (isUnlocked) {
                                        prefs.selectedCarId = car.id
                                    }
                                }
                                .testTag("car_card_${car.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                if (!isUnlocked) {
                                    Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Column {
                                    Text(
                                        text = car.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = car.category,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // BUY / SELECT BUTTON
                if (!carUnlocked) {
                    Button(
                        onClick = {
                            if (currentCoins >= currentCarSpec.price) {
                                prefs.coins -= currentCarSpec.price
                                currentCoins = prefs.coins
                                prefs.unlockCar(currentCarSpec.id)
                                prefs.selectedCarId = currentCarSpec.id
                                carUnlocked = true
                            }
                        },
                        enabled = currentCoins >= currentCarSpec.price,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("buy_car_button")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DESBLOQUEAR (${currentCarSpec.price} MONEDAS)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            prefs.selectedCarId = currentCarSpec.id
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (prefs.selectedCarId == currentCarSpec.id) Color(0xFF00C853) else Color(0xFF33384A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("select_car_button")
                    ) {
                        Icon(
                            imageVector = if (prefs.selectedCarId == currentCarSpec.id) Icons.Default.CheckCircle else Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (prefs.selectedCarId == currentCarSpec.id) "SELECCIONADO" else "ELEGIR ESTE COCHE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // RIGHT COLUMN: Tabs (Estadísticas, Pintura, Mejoras)
            Column(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight()
            ) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFF141722),
                    contentColor = Color(0xFFE50914)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Estadísticas", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Personalizar", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Tuning", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        0 -> { // Stats
                            Text(
                                text = currentCarSpec.description,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            StatBar(label = "Velocidad Máxima", value = (currentCarSpec.baseTopSpeed + (engineLevel - 1) * 18f) / 350f, display = "${(currentCarSpec.baseTopSpeed + (engineLevel - 1) * 18f).toInt()} KM/H", color = Color(0xFFE50914))
                            StatBar(label = "Aceleración", value = (currentCarSpec.baseAcceleration + (turboLevel - 1) * 1.5f) / 12f, display = "Nivel ${engineLevel + turboLevel}", color = Color(0xFFFF9800))
                            StatBar(label = "Manejo / Control", value = (currentCarSpec.baseHandling + (handlingLevel - 1) * 1.5f) / 12f, display = "${currentCarSpec.baseHandling}/10", color = Color(0xFF00E5FF))
                            StatBar(label = "Potencia de Derrape", value = currentCarSpec.baseDriftFactor / 10f, display = "${currentCarSpec.baseDriftFactor}/10", color = Color(0xFFFFD600))
                            StatBar(label = "Frenada Deportiva", value = (currentCarSpec.baseBraking + (brakesLevel - 1) * 2f) / 14f, display = "Nivel $brakesLevel", color = Color(0xFF76FF03))
                        }

                        1 -> { // Personalización: Pintura, Acabados, Vinilos/Decals y Llantas
                            CarCustomizerPanel(
                                carSpec = currentCarSpec,
                                prefs = prefs,
                                onColorChanged = { newColor ->
                                    selectedColor = newColor
                                },
                                onDecalChanged = { decal, decalColor ->
                                    selectedDecal = decal
                                    selectedDecalColor = decalColor
                                },
                                onWheelCustomized = { rimColor, caliperColor ->
                                    selectedRimColor = rimColor
                                    selectedCaliperColor = caliperColor
                                },
                                onCoinsUpdated = { updatedCoins ->
                                    currentCoins = updatedCoins
                                }
                            )
                        }

                        2 -> { // Tuning / Upgrades
                            UpgradeItem(
                                title = "Motor V-Spec",
                                subtitle = "Aumenta la velocidad punta",
                                currentLevel = engineLevel,
                                maxLevel = 5,
                                upgradeCost = engineLevel * 500,
                                canAfford = currentCoins >= engineLevel * 500,
                                onUpgrade = {
                                    val cost = engineLevel * 500
                                    if (currentCoins >= cost && engineLevel < 5) {
                                        prefs.coins -= cost
                                        currentCoins = prefs.coins
                                        engineLevel++
                                        prefs.setUpgradeLevel(currentCarSpec.id, "engine", engineLevel)
                                    }
                                }
                            )

                            UpgradeItem(
                                title = "Turbo & Inyección Nitro",
                                subtitle = "Mejora aceleración y recarga de NOS",
                                currentLevel = turboLevel,
                                maxLevel = 5,
                                upgradeCost = turboLevel * 500,
                                canAfford = currentCoins >= turboLevel * 500,
                                onUpgrade = {
                                    val cost = turboLevel * 500
                                    if (currentCoins >= cost && turboLevel < 5) {
                                        prefs.coins -= cost
                                        currentCoins = prefs.coins
                                        turboLevel++
                                        prefs.setUpgradeLevel(currentCarSpec.id, "turbo", turboLevel)
                                    }
                                }
                            )

                            UpgradeItem(
                                title = "Neumáticos y Suspensión",
                                subtitle = "Mayor agarre y control en derrapes",
                                currentLevel = handlingLevel,
                                maxLevel = 5,
                                upgradeCost = handlingLevel * 450,
                                canAfford = currentCoins >= handlingLevel * 450,
                                onUpgrade = {
                                    val cost = handlingLevel * 450
                                    if (currentCoins >= cost && handlingLevel < 5) {
                                        prefs.coins -= cost
                                        currentCoins = prefs.coins
                                        handlingLevel++
                                        prefs.setUpgradeLevel(currentCarSpec.id, "handling", handlingLevel)
                                    }
                                }
                            )

                            UpgradeItem(
                                title = "Frenos Cerámicos",
                                subtitle = "Respuesta de frenado instantánea",
                                currentLevel = brakesLevel,
                                maxLevel = 5,
                                upgradeCost = brakesLevel * 400,
                                canAfford = currentCoins >= brakesLevel * 400,
                                onUpgrade = {
                                    val cost = brakesLevel * 400
                                    if (currentCoins >= cost && brakesLevel < 5) {
                                        prefs.coins -= cost
                                        currentCoins = prefs.coins
                                        brakesLevel++
                                        prefs.setUpgradeLevel(currentCarSpec.id, "brakes", brakesLevel)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun StatBar(
    label: String,
    value: Float,
    display: String,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = display, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF232736))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .background(color)
            )
        }
    }
}

@Composable
private fun UpgradeItem(
    title: String,
    subtitle: String,
    currentLevel: Int,
    maxLevel: Int,
    upgradeCost: Int,
    canAfford: Boolean,
    onUpgrade: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E2230),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = subtitle, color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..maxLevel) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (i <= currentLevel) Color(0xFF00E5FF) else Color(0xFF33384A))
                        )
                    }
                }
            }

            if (currentLevel < maxLevel) {
                Button(
                    onClick = onUpgrade,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "MEJORAR", fontWeight = FontWeight.Black, fontSize = 10.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "$upgradeCost", fontSize = 9.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00C853)
                ) {
                    Text(
                        text = "MÁXIMO",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
