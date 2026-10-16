package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import com.example.game.engine.GameEngine
import com.example.game.engine.GameMap

@Composable
fun PauseDialog(
    engine: GameEngine,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "JUEGO EN PAUSA",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )

                // Map Quick Switcher
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2232))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CAMBIAR MAPA DE CARRERA:",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GameMap.entries.forEach { mapItem ->
                            val isCurrent = engine.activeMap == mapItem
                            val chipColor = when (mapItem) {
                                GameMap.NEON_CITY -> Color(0xFF00E5FF)
                                GameMap.COASTAL_SUNSET -> Color(0xFFFF9800)
                                GameMap.STUNT_DISTRICT -> Color(0xFF76FF03)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) chipColor.copy(alpha = 0.25f) else Color(0xFF12141C),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) chipColor else Color(0xFF282D3E)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        engine.changeMap(mapItem)
                                    }
                                    .testTag("pause_switch_map_${mapItem.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = when(mapItem) {
                                            GameMap.NEON_CITY -> "NEÓN"
                                            GameMap.COASTAL_SUNSET -> "COSTA"
                                            GameMap.STUNT_DISTRICT -> "RAMPAS"
                                        },
                                        color = if (isCurrent) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Stats summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222634))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatRow(label = "Distancia Recorrida", value = "${engine.distanceMeters.toInt()} m")
                    StatRow(label = "Puntos Totales", value = "${engine.totalScore} pts")
                    StatRow(label = "Puntos de Derrape", value = "${engine.driftScore} pts")
                    StatRow(label = "Monedas Obtenidas", value = "+${engine.coinsEarned}")
                }

                // Action buttons
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONTINUAR", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestart,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restart_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REINICIAR PARTIDA", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onExitToMenu,
                    modifier = Modifier.fillMaxWidth().testTag("exit_menu_button")
                ) {
                    Text("SALIR AL MENÚ PRINCIPAL", color = Color(0xFFAAAAAA))
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    engine: GameEngine,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Dialog(onDismissRequest = onExitToMenu) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "¡CARRERA FINALIZADA!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )

                // Results Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222634))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatRow(label = "Puntuación Final", value = "${engine.totalScore} PTS", highlight = true)
                    StatRow(label = "Distancia Total", value = "${engine.distanceMeters.toInt()} m")
                    StatRow(label = "Derrape Total", value = "${engine.driftScore} pts")
                    StatRow(label = "Adelantamientos", value = "${engine.trackWorld.closeCallsCount}")
                    StatRow(label = "Monedas Ganadas", value = "+${engine.coinsEarned}", coin = true)
                }

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("play_again_button")
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("JUGAR DE NUEVO", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onExitToMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3446)),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("garage_menu_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IR AL GARAJE / MENÚ", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    coin: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (coin) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                color = if (coin) Color(0xFFFFD700) else if (highlight) Color(0xFF00E5FF) else Color.White,
                fontSize = if (highlight) 16.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
