package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.game.engine.*

/**
 * UI component allowing players to modify car colors, finishes, decals/liveries,
 * and wheel/caliper styling with instant live 3D preview.
 */
@Composable
fun CarCustomizerPanel(
    carSpec: CarSpec,
    prefs: GamePreferences,
    onColorChanged: (Long) -> Unit,
    onDecalChanged: (DecalType, Long) -> Unit,
    onWheelCustomized: (Long, Long) -> Unit,
    onCoinsUpdated: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Pintura/Acabado, 1 = Vinilos & Decals, 2 = Llantas y Pinzas

    var selectedColor by remember(carSpec.id) {
        mutableLongStateOf(prefs.getCarColor(carSpec.id, carSpec.defaultColorHex))
    }
    var selectedFinish by remember(carSpec.id) {
        mutableStateOf(prefs.getCarFinish(carSpec.id))
    }
    var selectedDecal by remember(carSpec.id) {
        mutableStateOf(prefs.getCarDecal(carSpec.id))
    }
    var selectedDecalColor by remember(carSpec.id) {
        mutableLongStateOf(prefs.getCarDecalColor(carSpec.id))
    }
    var selectedRimColor by remember(carSpec.id) {
        mutableLongStateOf(prefs.getCarRimColor(carSpec.id))
    }
    var selectedCaliperColor by remember(carSpec.id) {
        mutableLongStateOf(prefs.getCarCaliperColor(carSpec.id))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // SUB-NAVIGATION PILLS (Pintura, Vinilos, Llantas)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomizerPill(
                title = "Pintura",
                icon = Icons.Default.Palette,
                isSelected = subTab == 0,
                onClick = { subTab = 0 },
                modifier = Modifier.weight(1f).testTag("customizer_tab_paint")
            )
            CustomizerPill(
                title = "Vinilos / Decals",
                icon = Icons.Default.AutoFixHigh,
                isSelected = subTab == 1,
                onClick = { subTab = 1 },
                modifier = Modifier.weight(1.3f).testTag("customizer_tab_decals")
            )
            CustomizerPill(
                title = "Llantas",
                icon = Icons.Default.DonutLarge,
                isSelected = subTab == 2,
                onClick = { subTab = 2 },
                modifier = Modifier.weight(1f).testTag("customizer_tab_wheels")
            )
        }

        // SUB-TAB CONTENTS
        when (subTab) {
            0 -> {
                // PINTURA & ACABADO
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ACABADO DE LA PINTURA",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    // Finish Selector Chips (Metálico, Mate, Cromado, Neón)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaintFinish.values().forEach { finish ->
                            val isSelected = selectedFinish == finish
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFE50914) else Color(0xFF1E2230),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedFinish = finish
                                        prefs.setCarFinish(carSpec.id, finish)
                                    }
                                    .testTag("finish_chip_${finish.id}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = finish.title,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "COLOR DE LA CARROCERÍA",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    // 12 Preset Paint Colors
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CarCatalog.PRESET_COLORS.take(6).forEach { (colorHex, name) ->
                                PaintColorCircle(
                                    colorHex = colorHex,
                                    name = name,
                                    isSelected = selectedColor == colorHex,
                                    onClick = {
                                        selectedColor = colorHex
                                        prefs.setCarColor(carSpec.id, colorHex)
                                        onColorChanged(colorHex)
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CarCatalog.PRESET_COLORS.drop(6).forEach { (colorHex, name) ->
                                PaintColorCircle(
                                    colorHex = colorHex,
                                    name = name,
                                    isSelected = selectedColor == colorHex,
                                    onClick = {
                                        selectedColor = colorHex
                                        prefs.setCarColor(carSpec.id, colorHex)
                                        onColorChanged(colorHex)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // VINILOS & CALCOMANÍAS (DECALS & LIVERIES)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DISEÑOS DE VINILOS Y CALCOMANÍAS",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    // Decal design cards
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DecalType.values().forEach { decal ->
                            val isSelected = selectedDecal == decal
                            val isUnlocked = prefs.isDecalUnlocked(carSpec.id, decal)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF252A3C) else Color(0xFF1E2230),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isUnlocked) {
                                            selectedDecal = decal
                                            prefs.setCarDecal(carSpec.id, decal)
                                            onDecalChanged(decal, selectedDecalColor)
                                        }
                                    }
                                    .testTag("decal_card_${decal.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF2B3042)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (decal == DecalType.NONE) Icons.Default.Close else Icons.Default.Brush,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.Black else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = decal.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = decal.subtitle,
                                                color = Color.Gray,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    if (!isUnlocked) {
                                        Button(
                                            onClick = {
                                                if (prefs.coins >= decal.price) {
                                                    prefs.coins -= decal.price
                                                    onCoinsUpdated(prefs.coins)
                                                    prefs.unlockDecal(carSpec.id, decal)
                                                    selectedDecal = decal
                                                    prefs.setCarDecal(carSpec.id, decal)
                                                    onDecalChanged(decal, selectedDecalColor)
                                                }
                                            },
                                            enabled = prefs.coins >= decal.price,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(30.dp)
                                                .testTag("unlock_decal_${decal.id}")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = "${decal.price}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Aplicado", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Decal color selector (if decal is selected)
                    if (selectedDecal != DecalType.NONE) {
                        Text(
                            text = "COLOR DEL VINILO / FRANJA",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CarCatalog.DECAL_COLORS.forEach { (colorHex, name) ->
                                PaintColorCircle(
                                    colorHex = colorHex,
                                    name = name,
                                    isSelected = selectedDecalColor == colorHex,
                                    onClick = {
                                        selectedDecalColor = colorHex
                                        prefs.setCarDecalColor(carSpec.id, colorHex)
                                        onDecalChanged(selectedDecal, colorHex)
                                    },
                                    tagPrefix = "decal_color_"
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // LLANTAS Y PINZAS DE FRENO
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "COLOR DE LAS LLANTAS DE ALEACIÓN",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CarCatalog.RIM_COLORS.forEach { (colorHex, name) ->
                            PaintColorCircle(
                                colorHex = colorHex,
                                name = name,
                                isSelected = selectedRimColor == colorHex,
                                onClick = {
                                    selectedRimColor = colorHex
                                    prefs.setCarRimColor(carSpec.id, colorHex)
                                    onWheelCustomized(colorHex, selectedCaliperColor)
                                },
                                tagPrefix = "rim_color_"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "COLOR DE LAS PINZAS DE FRENO DEPORTIVAS",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CarCatalog.CALIPER_COLORS.forEach { (colorHex, name) ->
                            PaintColorCircle(
                                colorHex = colorHex,
                                name = name,
                                isSelected = selectedCaliperColor == colorHex,
                                onClick = {
                                    selectedCaliperColor = colorHex
                                    prefs.setCarCaliperColor(carSpec.id, colorHex)
                                    onWheelCustomized(selectedRimColor, colorHex)
                                },
                                tagPrefix = "caliper_color_"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizerPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFE50914) else Color(0xFF1E2230),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun PaintColorCircle(
    colorHex: Long,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tagPrefix: String = "color_chip_"
) {
    val chipColor = Color(colorHex or 0xFF000000)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp)
            .testTag("$tagPrefix${colorHex.toString(16)}")
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(chipColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF),
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = name,
            color = if (isSelected) Color(0xFF00E5FF) else Color.Gray,
            fontSize = 8.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
