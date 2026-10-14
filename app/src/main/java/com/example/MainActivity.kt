package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.game.engine.GameMode
import com.example.game.engine.GamePreferences
import com.example.game.ui.GamePlayScreen
import com.example.game.ui.GarageScreen
import com.example.game.ui.MainMenuScreen
import com.example.game.ui.SettingsScreen

enum class AppScreen {
    MENU,
    GARAGE,
    SETTINGS,
    GAME
}

class MainActivity : ComponentActivity() {

    private lateinit var prefs: GamePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        enableEdgeToEdge()
        prefs = GamePreferences(this)

        setContent {
            ApexDriveTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.MENU) }
                var activeGameMode by remember { mutableStateOf(GameMode.FREE_ROAM) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0C12))
                ) {
                    when (currentScreen) {
                        AppScreen.MENU -> {
                            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                                MainMenuScreen(
                                    prefs = prefs,
                                    onSelectMode = { mode ->
                                        activeGameMode = mode
                                        currentScreen = AppScreen.GAME
                                    },
                                    onOpenGarage = { currentScreen = AppScreen.GARAGE },
                                    onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                                )
                            }
                        }

                        AppScreen.GARAGE -> {
                            BackHandler { currentScreen = AppScreen.MENU }
                            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                                GarageScreen(
                                    prefs = prefs,
                                    onBack = { currentScreen = AppScreen.MENU }
                                )
                            }
                        }

                        AppScreen.SETTINGS -> {
                            BackHandler { currentScreen = AppScreen.MENU }
                            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                                SettingsScreen(
                                    prefs = prefs,
                                    onBack = { currentScreen = AppScreen.MENU }
                                )
                            }
                        }

                        AppScreen.GAME -> {
                            // Immersive Game Screen (no extra safeDrawingPadding so 3D world fills entire viewport)
                            GamePlayScreen(
                                mode = activeGameMode,
                                prefs = prefs,
                                onExitToMenu = { currentScreen = AppScreen.MENU }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApexDriveTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFFE50914),
        onPrimary = Color.White,
        secondary = Color(0xFF00E5FF),
        background = Color(0xFF0A0C12),
        surface = Color(0xFF141724)
    )

    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}
