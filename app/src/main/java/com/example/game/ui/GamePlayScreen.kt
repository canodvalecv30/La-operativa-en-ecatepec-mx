package com.example.game.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.engine.GameEngine
import com.example.game.engine.GameMode
import com.example.game.engine.GamePreferences
import com.example.game.engine.SteeringType

@Composable
fun GamePlayScreen(
    mode: GameMode,
    prefs: GamePreferences,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var glSurfaceViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }

    val engine = remember(mode) {
        GameEngine(context, prefs, mode).apply {
            start()
        }
    }

    // Accelerometer listener for Tilt steering
    DisposableEffect(prefs.steeringType) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && prefs.steeringType == SteeringType.TILT_SENSOR) {
                    // event.values[0] is X-axis tilt
                    val tiltX = -event.values[0] * prefs.tiltSensitivity * 0.22f
                    engine.inputSteer = tiltX.coerceIn(-1.0f, 1.0f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (prefs.steeringType == SteeringType.TILT_SENSOR && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    DisposableEffect(engine) {
        onDispose {
            engine.destroy()
        }
    }

    // Intercept back button to show pause menu
    BackHandler {
        if (!engine.isGameOver) {
            engine.pause()
        } else {
            onExitToMenu()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Hardware Accelerated OpenGL ES 3D Surface View
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(engine.renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glSurfaceViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Heads-Up Display & Interactive Racing Controls
        GameHud(
            engine = engine,
            onPauseClick = { engine.pause() }
        )

        // 3. Pause Dialog
        if (engine.isPaused && !engine.isGameOver) {
            PauseDialog(
                engine = engine,
                onResume = { engine.resume() },
                onRestart = {
                    engine.start()
                },
                onExitToMenu = onExitToMenu
            )
        }

        // 4. Game Over Dialog
        if (engine.isGameOver) {
            GameOverDialog(
                engine = engine,
                onRestart = {
                    engine.start()
                },
                onExitToMenu = onExitToMenu
            )
        }
    }
}
