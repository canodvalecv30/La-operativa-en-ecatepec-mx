package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GamePreferences
import com.example.game.engine.GraphicsQuality
import com.example.game.engine.SteeringType

@Composable
fun SettingsScreen(
    prefs: GamePreferences,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var steeringType by remember { mutableStateOf(prefs.steeringType) }
    var graphicsQuality by remember { mutableStateOf(prefs.graphicsQuality) }
    var characterSkin by remember { mutableStateOf(prefs.characterSkin) }
    var roadReflections by remember { mutableStateOf(prefs.roadReflectionsEnabled) }
    var bloomEnabled by remember { mutableStateOf(prefs.bloomEnabled) }
    var soundEnabled by remember { mutableStateOf(prefs.soundEnabled) }
    var hapticsEnabled by remember { mutableStateOf(prefs.hapticsEnabled) }
    var tiltSensitivity by remember { mutableFloatStateOf(prefs.tiltSensitivity) }
    var coins by remember { mutableIntStateOf(prefs.coins) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1118))
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2230))
                    .testTag("settings_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "AJUSTES, CONTROLES Y GRÁFICOS",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp
            )
        }

        // 2-COLUMN LANDSCAPE LAYOUT
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LEFT COLUMN: Controls & Steering
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(title = "MÉTODO DE CONDUCCIÓN", icon = Icons.Default.SportsMotorsports)

                SteeringOptionCard(
                    title = "Botones en Pantalla",
                    subtitle = "Flechas izquierda y derecha precisas",
                    icon = Icons.Default.TouchApp,
                    isSelected = steeringType == SteeringType.BUTTONS,
                    onClick = {
                        steeringType = SteeringType.BUTTONS
                        prefs.steeringType = SteeringType.BUTTONS
                    }
                )

                SteeringOptionCard(
                    title = "Volante Virtual",
                    subtitle = "Gira el volante táctil en pantalla",
                    icon = Icons.Default.DirectionsCar,
                    isSelected = steeringType == SteeringType.STEERING_WHEEL,
                    onClick = {
                        steeringType = SteeringType.STEERING_WHEEL
                        prefs.steeringType = SteeringType.STEERING_WHEEL
                    }
                )

                SteeringOptionCard(
                    title = "Giroscopio / Inclinación",
                    subtitle = "Inclina el móvil a los lados para maniobrar",
                    icon = Icons.Default.ScreenRotation,
                    isSelected = steeringType == SteeringType.TILT_SENSOR,
                    onClick = {
                        steeringType = SteeringType.TILT_SENSOR
                        prefs.steeringType = SteeringType.TILT_SENSOR
                    }
                )

                if (steeringType == SteeringType.TILT_SENSOR) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E2230),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Sensibilidad", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = String.format("%.1fx", tiltSensitivity), color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = tiltSensitivity,
                                onValueChange = {
                                    tiltSensitivity = it
                                    prefs.tiltSensitivity = it
                                },
                                valueRange = 0.5f..2.5f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                            )
                        }
                    }
                }

                // Audio & Haptics
                SectionHeader(title = "AUDIO Y VIBRACIÓN", icon = Icons.AutoMirrored.Filled.VolumeUp)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2230),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Efectos de Sonido y Motor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Sonido dinámico de RPM, turbo y derrape", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = {
                                    soundEnabled = it
                                    prefs.soundEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE50914), checkedTrackColor = Color(0x66E50914))
                            )
                        }

                        HorizontalDivider(color = Color(0xFF282D3E))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Vibración Háptica", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("En choques, aceleración y derrapes", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = hapticsEnabled,
                                onCheckedChange = {
                                    hapticsEnabled = it
                                    prefs.hapticsEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE50914), checkedTrackColor = Color(0x66E50914))
                            )
                        }
                    }
                }
            }

            // RIGHT COLUMN: Graphics Quality & Optimization + Records
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionHeader(title = "CALIDAD GRÁFICA Y RENDIMIENTO", icon = Icons.Default.Tune)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GraphicsQuality.values().forEach { gq ->
                        val isSelected = graphicsQuality == gq
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF252A3C) else Color(0xFF1E2230),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    graphicsQuality = gq
                                    prefs.graphicsQuality = gq
                                }
                                .testTag("quality_option_${gq.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = gq.title,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = gq.description,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Granular Optimization Toggles
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2230),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Reflejos de Asfalto (PBR)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Brillo realista en pista según el ángulo de visión", color = Color.Gray, fontSize = 9.sp)
                            }
                            Switch(
                                checked = roadReflections,
                                onCheckedChange = {
                                    roadReflections = it
                                    prefs.roadReflectionsEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                            )
                        }

                        HorizontalDivider(color = Color(0xFF282D3E))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Efectos de Brillo / Luces", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Proyectores de faros duales y emisión de neón", color = Color.Gray, fontSize = 9.sp)
                            }
                            Switch(
                                checked = bloomEnabled,
                                onCheckedChange = {
                                    bloomEnabled = it
                                    prefs.bloomEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x6600E5FF))
                            )
                        }
                    }
                }

                // 3D PILOT & REALISTIC CHARACTER SKINS
                SectionHeader(title = "EQUIPAMIENTO DE PILOTO 3D", icon = Icons.Default.Person)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.game.engine.CharacterSkin.values().forEach { skin ->
                        val isSelected = characterSkin == skin
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF252A3C) else Color(0xFF1E2230),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE50914)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    characterSkin = skin
                                    prefs.characterSkin = skin
                                }
                                .testTag("pilot_skin_${skin.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Suit & Visor color dots preview
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(skin.suitColorHex or 0xFF000000))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(skin.visorColorHex or 0xFF000000))
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = skin.title,
                                        color = if (isSelected) Color(0xFFE50914) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = skin.subtitle,
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                SectionHeader(title = "RÉCORDS Y BANCO", icon = Icons.Default.EmojiEvents)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E2230),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatDisplayRow(label = "Récord Autopista Rush", value = "${prefs.bestDistanceRush.toInt()} m")
                        StatDisplayRow(label = "Mejor Derrape Acumulado", value = "${prefs.bestDriftScore} pts")
                        StatDisplayRow(label = "Monedas en Banco", value = "$coins monedas")
                    }
                }

                Button(
                    onClick = {
                        prefs.coins += 2500
                        coins = prefs.coins
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A7E19)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("bonus_coins_button")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RECLAMAR +2500 MONEDAS GRATIS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = Color(0xFFE50914),
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SteeringOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF252A3C) else Color(0xFF1E2230),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE50914)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFFE50914) else Color(0xFF2B3042)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = subtitle, color = Color.Gray, fontSize = 10.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StatDisplayRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 11.sp)
        Text(value, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
