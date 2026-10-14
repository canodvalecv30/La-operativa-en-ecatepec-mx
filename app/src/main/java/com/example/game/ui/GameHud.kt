package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.game.engine.GameMode
import com.example.game.engine.SteeringType
import kotlin.math.*

@Composable
fun GameHud(
    engine: GameEngine,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steeringType = engine.prefs.steeringType
    val isOnFoot = engine.isOnFoot

    Box(modifier = modifier.fillMaxSize()) {
        // TOP BAR: Stats, Score, Time/Distance, Get In/Out Vehicle, Mini-controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Score & Coins
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC12141C))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PUNTOS: ${engine.totalScore}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Monedas",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${engine.coinsEarned}",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Center: Mode-specific info or On-foot status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC12141C))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                if (isOnFoot) {
                    Text(
                        text = "EXPLORACIÓN A PIE",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    val distToCar = engine.characterPhysics.distanceToCar(engine.carPhysics).toInt()
                    Text(
                        text = "Coche a $distToCar metros",
                        color = if (engine.canEnterCar) Color(0xFF00E676) else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    when (engine.mode) {
                        GameMode.TIME_ATTACK, GameMode.DRIFT_MASTER -> {
                            Text(
                                text = "TIEMPO: ${engine.timeRemainingSeconds.toInt()}s",
                                color = if (engine.timeRemainingSeconds < 15f) Color(0xFFFF3D00) else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        else -> {
                            Text(
                                text = "${engine.distanceMeters.toInt()} METROS",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Right: Bajar/Subir Coche, Camera, Reset, Pause buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BAJARSE DEL VEHÍCULO BUTTON (When driving)
                if (!isOnFoot) {
                    Button(
                        onClick = { engine.exitVehicle() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("exit_vehicle_button")
                    ) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BAJARSE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Reset Car / Character (if stuck)
                IconButton(
                    onClick = { engine.resetCarPosition() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC12141C))
                        .testTag("reset_car_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reiniciar Posición", tint = Color.White)
                }

                // Camera Switch
                IconButton(
                    onClick = { engine.toggleCamera() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC12141C))
                        .testTag("switch_cam_button")
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Cambiar Cámara", tint = Color.White)
                }

                // Pause
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC12141C))
                        .testTag("pause_game_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = Color.White)
                }
            }
        }

        // EVENT POPUP NOTICES (Crash, Close Call, Stunt Jump, Drift, On Foot)
        AnimatedVisibility(
            visible = engine.currentEventNotice != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            engine.currentEventNotice?.let { notice ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xEEFF3D00),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = notice.text,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // DRIFT SCORE FLOATING BADGE (when driving)
        if (!isOnFoot && engine.isDrifting) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xD0FFD600),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-60).dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "¡DERRAPE!",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "+${engine.driftScore} PTS",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // BOTTOM CONTROLS
        if (isOnFoot) {
            // ON-FOOT CONTROLS (Joystick, Sprint, Jump, Enter Car)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT: 360° Virtual Analog Joystick
                VirtualJoystick(
                    onMove = { x, z ->
                        engine.inputMoveX = x
                        engine.inputMoveZ = z
                    },
                    modifier = Modifier.size(135.dp)
                )

                // CENTER: Proximity Prompt to Enter Car
                if (engine.canEnterCar) {
                    Button(
                        onClick = { engine.enterVehicle() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .padding(bottom = 8.dp)
                            .testTag("enter_vehicle_button")
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SUBIR AL COCHE",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }

                // RIGHT: Actions (Sprint & Jump)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Sprint / Correr Button
                    HoldButton(
                        onPressed = { engine.inputSprint = true },
                        onReleased = { engine.inputSprint = false },
                        backgroundColor = if (engine.characterPhysics.isSprinting) Color(0xFF00E5FF) else Color(0xCC2A2D3A),
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("sprint_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.DirectionsRun,
                                contentDescription = "Correr",
                                tint = if (engine.characterPhysics.isSprinting) Color.Black else Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                "CORRER",
                                color = if (engine.characterPhysics.isSprinting) Color.Black else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Jump / Saltar Button
                    Button(
                        onClick = { engine.characterJump() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("jump_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Saltar", tint = Color.White, modifier = Modifier.size(28.dp))
                            Text("SALTAR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            // DRIVING CONTROLS (Steer/Wheel, Tachometer, Pedals, Handbrake, Nitro)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT CONTROL: Steering (Buttons or Steering Wheel)
                when (steeringType) {
                    SteeringType.STEERING_WHEEL -> {
                        VirtualSteeringWheel(
                            onSteer = { engine.inputSteer = it },
                            modifier = Modifier.size(150.dp)
                        )
                    }
                    SteeringType.TILT_SENSOR -> {
                        Box(
                            modifier = Modifier
                                .size(120.dp, 60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x9912141C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GIROSCOPIO ACTIVO\nIncline el dispositivo",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        // Classic Left / Right Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HoldButton(
                                onPressed = { engine.inputSteer = -1.0f },
                                onReleased = { engine.inputSteer = 0f },
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("steer_left_button")
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Girar Izquierda",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            HoldButton(
                                onPressed = { engine.inputSteer = 1.0f },
                                onReleased = { engine.inputSteer = 0f },
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("steer_right_button")
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "Girar Derecha",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                // CENTER: INSTRUMENT CLUSTER (Speedometer, RPM, Gear, Nitro Gauge)
                InstrumentCluster(
                    speedKmH = engine.currentSpeedKmH,
                    rpm = engine.currentRpm,
                    gear = engine.currentGear,
                    nitroPercent = engine.nitroPercent,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // RIGHT CONTROLS: Gas Pedal, Brake Pedal, Handbrake, Nitro
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top row: Handbrake and Nitro
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HoldButton(
                            onPressed = { engine.inputHandbrake = true },
                            onReleased = { engine.inputHandbrake = false },
                            backgroundColor = Color(0xCCFF9800),
                            modifier = Modifier
                                .size(58.dp)
                                .testTag("handbrake_button")
                        ) {
                            Text("DRIFT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }

                        HoldButton(
                            onPressed = { engine.inputNitro = true },
                            onReleased = { engine.inputNitro = false },
                            backgroundColor = if (engine.nitroPercent > 5f) Color(0xDD00E5FF) else Color(0x5500E5FF),
                            modifier = Modifier
                                .size(58.dp)
                                .testTag("nitro_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Bolt, contentDescription = "Nitro", tint = Color.White, modifier = Modifier.size(24.dp))
                                Text("NOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }
                    }

                    // Bottom row: Brake Pedal & Gas Pedal
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HoldButton(
                            onPressed = { engine.inputBrake = 1.0f },
                            onReleased = { engine.inputBrake = 0f },
                            backgroundColor = Color(0xCCE50914),
                            modifier = Modifier
                                .width(62.dp)
                                .height(84.dp)
                                .testTag("brake_pedal_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Stop, contentDescription = "Freno", tint = Color.White, modifier = Modifier.size(26.dp))
                                Text("FRENO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }

                        HoldButton(
                            onPressed = { engine.inputThrottle = 1.0f },
                            onReleased = { engine.inputThrottle = 0f },
                            backgroundColor = Color(0xCC00C853),
                            modifier = Modifier
                                .width(68.dp)
                                .height(100.dp)
                                .testTag("gas_pedal_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Acelerador", tint = Color.White, modifier = Modifier.size(34.dp))
                                Text("GAS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x881E212D))
            .border(2.dp, Color(0x5500E5FF), CircleShape)
            .pointerInput(Unit) {
                val radiusPx = 50.dp.toPx()
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val distance = newOffset.getDistance()
                        val clamped = if (distance > radiusPx) {
                            newOffset * (radiusPx / distance)
                        } else {
                            newOffset
                        }
                        thumbOffset = clamped
                        onMove(clamped.x / radiusPx, -clamped.y / radiusPx)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
    ) {
        // Inner thumb knob
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DirectionsWalk, contentDescription = "Mover", tint = Color.Black, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun InstrumentCluster(
    speedKmH: Float,
    rpm: Float,
    gear: Int,
    nitroPercent: Float,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(136.dp)
            .clip(CircleShape)
            .background(Color(0xDD0D0F16))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeW = 8.dp.toPx()
            drawArc(
                color = Color(0x33FFFFFF),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            val rpmRatio = ((rpm - 1000f) / 7000f).coerceIn(0f, 1f)
            val arcColor = when {
                rpmRatio > 0.85f -> Color(0xFFFF1744)
                rpmRatio > 0.65f -> Color(0xFFFFD600)
                else -> Color(0xFF00E5FF)
            }
            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = 270f * rpmRatio,
                useCenter = false,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${speedKmH.toInt()}",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp
            )
            Text(
                text = "KM/H",
                color = Color(0xFFAAAAAA),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE50914)
                ) {
                    Text(
                        text = if (gear == -1) "R" else "D$gear",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x4400E5FF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(nitroPercent / 100f)
                            .background(Color(0xFF00E5FF))
                    )
                }
            }
        }
    }
}

@Composable
fun HoldButton(
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xCC2A2D3A),
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 4.dp,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressed()
                        tryAwaitRelease()
                        onReleased()
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun VirtualSteeringWheel(
    onSteer: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        rotationAngle = (rotationAngle + dragAmount.x * 0.8f).coerceIn(-90f, 90f)
                        onSteer(rotationAngle / 90f)
                    },
                    onDragEnd = {
                        rotationAngle = 0f
                        onSteer(0f)
                    },
                    onDragCancel = {
                        rotationAngle = 0f
                        onSteer(0f)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E212D), Color(0xFF0A0C11))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Color(0xFF373B4D))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(14.dp)
                    .background(Color(0xFF373B4D))
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Navigation, contentDescription = "Apex", tint = Color.White)
            }
        }
    }
}
