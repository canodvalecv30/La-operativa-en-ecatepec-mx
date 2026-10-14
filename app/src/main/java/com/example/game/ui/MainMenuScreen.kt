package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.CarCatalog
import com.example.game.engine.GameMode
import com.example.game.engine.GamePreferences

@Composable
fun MainMenuScreen(
    prefs: GamePreferences,
    onSelectMode: (GameMode) -> Unit,
    onOpenGarage: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCar = CarCatalog.CARS.find { it.id == prefs.selectedCarId } ?: CarCatalog.CARS[0]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0C12),
                        Color(0xFF141724),
                        Color(0xFF090B10)
                    )
                )
            )
    ) {
        // Landscape 2-column layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LEFT COLUMN: Header, Branding, Selected Car & Actions
            Column(
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Brand & Coins
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE50914)
                        ) {
                            Text(
                                text = "3D MOTOR RACING",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Coins & Settings
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E2232),
                                modifier = Modifier.testTag("coins_indicator")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "Monedas",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${prefs.coins}",
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2232))
                                    .testTag("menu_settings_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "APEX DRIVE 3D",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Simulador de Carreras y Conducción",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                // Selected Car Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B2030),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenGarage)
                        .testTag("car_preview_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE50914)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = selectedCar.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = selectedCar.category,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = onOpenGarage,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282F45)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_garage_button")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "GARAJE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // RIGHT COLUMN: Game Modes Grid
            Column(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SELECCIONA MODO DE JUEGO",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                GameModeCard(
                    mode = GameMode.FREE_ROAM,
                    title = "MODO LIBRE / ACROBACIAS",
                    description = "Conduce y bájate a pie a explorar la ciudad y rampas.",
                    icon = Icons.Default.Explore,
                    accentColor = Color(0xFF00E5FF),
                    onClick = { onSelectMode(GameMode.FREE_ROAM) }
                )

                GameModeCard(
                    mode = GameMode.TRAFFIC_RUSH,
                    title = "TRÁFICO AUTOPISTA",
                    description = "Esquiva autos a más de 200 km/h y adelanta al límite.",
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFFE50914),
                    onClick = { onSelectMode(GameMode.TRAFFIC_RUSH) }
                )

                GameModeCard(
                    mode = GameMode.TIME_ATTACK,
                    title = "CONTRARRELOJ (90s)",
                    description = "Recorre la máxima distancia antes de que el cronómetro llegue a 0.",
                    icon = Icons.Default.Timer,
                    accentColor = Color(0xFFFF9800),
                    onClick = { onSelectMode(GameMode.TIME_ATTACK) }
                )

                GameModeCard(
                    mode = GameMode.DRIFT_MASTER,
                    title = "MAESTRO DEL DERRAPE",
                    description = "Usa el freno de mano y encadena combos de drift en curvas.",
                    icon = Icons.Default.AutoFixHigh,
                    accentColor = Color(0xFFFFD600),
                    onClick = { onSelectMode(GameMode.DRIFT_MASTER) }
                )
            }
        }
    }
}

@Composable
private fun GameModeCard(
    mode: GameMode,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF161925),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D3E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mode_card_${mode.name}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
