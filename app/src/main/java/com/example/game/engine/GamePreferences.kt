package com.example.game.engine

import android.content.Context
import android.content.SharedPreferences

enum class GameMode(val title: String, val description: String) {
    FREE_ROAM("Modo Libre", "Conduce sin límites por la ciudad y pista de acrobacias."),
    TRAFFIC_RUSH("Tráfico Autopista", "Esquiva tráfico a máxima velocidad. ¡Adelanta al límite!"),
    TIME_ATTACK("Contrarreloj", "Completa vueltas al circuito antes de que acabe el tiempo."),
    DRIFT_MASTER("Maestro del Derrape", "Consigue la máxima puntuación encadenando derrapes.")
}

enum class SteeringType {
    BUTTONS,
    STEERING_WHEEL,
    TILT_SENSOR
}

data class CarSpec(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val defaultColorHex: Long,
    val baseTopSpeed: Float, // km/h
    val baseAcceleration: Float,
    val baseHandling: Float,
    val baseDriftFactor: Float,
    val baseBraking: Float,
    val description: String
)

data class CarCustomization(
    val carId: String,
    val colorHex: Long,
    val engineLevel: Int,
    val handlingLevel: Int,
    val turboLevel: Int,
    val brakesLevel: Int
)

object CarCatalog {
    val CARS = listOf(
        CarSpec(
            id = "apex_gt",
            name = "Apex GT-R",
            category = "Superdeportivo",
            price = 0, // Car starter
            defaultColorHex = 0xFFE50914, // Racing Red
            baseTopSpeed = 260f,
            baseAcceleration = 8.5f,
            baseHandling = 7.5f,
            baseDriftFactor = 8.0f,
            baseBraking = 8.0f,
            description = "Equilibrado, veloz y con una maniobrabilidad excepcional en curvas."
        ),
        CarSpec(
            id = "muscle_v8",
            name = "Thunder V8",
            category = "Muscle Car",
            price = 2500,
            defaultColorHex = 0xFFFF9800, // Vibrant Orange
            baseTopSpeed = 280f,
            baseAcceleration = 9.5f,
            baseHandling = 6.5f,
            baseDriftFactor = 9.2f,
            baseBraking = 7.0f,
            description = "Puro músculo americano. Aceleración brutal y derrapes salvajes."
        ),
        CarSpec(
            id = "tokyo_drift",
            name = "Shinobi R34",
            category = "JDM Tuner",
            price = 6000,
            defaultColorHex = 0xFF00E5FF, // Cyber Cyan
            baseTopSpeed = 270f,
            baseAcceleration = 8.8f,
            baseHandling = 9.4f,
            baseDriftFactor = 9.8f,
            baseBraking = 8.5f,
            description = "La máquina definitiva para el derrape. Control milimétrico en curvas."
        ),
        CarSpec(
            id = "hyper_phantom",
            name = "Phantom V12",
            category = "Hiperdeportivo",
            price = 12000,
            defaultColorHex = 0xFF7C4DFF, // Neon Purple
            baseTopSpeed = 330f,
            baseAcceleration = 10.0f,
            baseHandling = 9.0f,
            baseDriftFactor = 8.5f,
            baseBraking = 9.5f,
            description = "Ingeniería de máxima competición. Velocidad extrema en recta."
        )
    )

    val PRESET_COLORS = listOf(
        0xFFE50914 to "Rojo Furia",
        0xFF00E5FF to "Azul Neón",
        0xFFFFD600 to "Amarillo Oro",
        0xFF76FF03 to "Verde Lima",
        0xFF7C4DFF to "Púrpura Viper",
        0xFFFF3D00 to "Naranja Lava",
        0xFF212121 to "Negro Carbón",
        0xFFECEFF1 to "Blanco Perla"
    )
}

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("apex_drive_prefs", Context.MODE_PRIVATE)

    var coins: Int
        get() = prefs.getInt("coins", 1500)
        set(value) = prefs.edit().putInt("coins", value).apply()

    var selectedCarId: String
        get() = prefs.getString("selected_car_id", "apex_gt") ?: "apex_gt"
        set(value) = prefs.edit().putString("selected_car_id", value).apply()

    fun isCarUnlocked(carId: String): Boolean {
        if (carId == "apex_gt") return true
        return prefs.getBoolean("car_unlocked_$carId", false)
    }

    fun unlockCar(carId: String) {
        prefs.edit().putBoolean("car_unlocked_$carId", true).apply()
    }

    fun getCarColor(carId: String, defaultColor: Long): Long {
        return prefs.getLong("car_color_$carId", defaultColor)
    }

    fun setCarColor(carId: String, color: Long) {
        prefs.edit().putLong("car_color_$carId", color).apply()
    }

    fun getUpgradeLevel(carId: String, upgradeType: String): Int {
        return prefs.getInt("car_upgrade_${carId}_$upgradeType", 1)
    }

    fun setUpgradeLevel(carId: String, upgradeType: String, level: Int) {
        prefs.edit().putInt("car_upgrade_${carId}_$upgradeType", level).apply()
    }

    var steeringType: SteeringType
        get() {
            val name = prefs.getString("steering_type", SteeringType.BUTTONS.name)
            return try { SteeringType.valueOf(name ?: SteeringType.BUTTONS.name) } catch (e: Exception) { SteeringType.BUTTONS }
        }
        set(value) = prefs.edit().putString("steering_type", value.name).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var tiltSensitivity: Float
        get() = prefs.getFloat("tilt_sensitivity", 1.2f)
        set(value) = prefs.edit().putFloat("tilt_sensitivity", value).apply()

    var bestDistanceRush: Float
        get() = prefs.getFloat("best_distance_rush", 0f)
        set(value) = prefs.edit().putFloat("best_distance_rush", value).apply()

    var bestLapTime: Float
        get() = prefs.getFloat("best_lap_time", 999.9f)
        set(value) = prefs.edit().putFloat("best_lap_time", value).apply()

    var bestDriftScore: Int
        get() = prefs.getInt("best_drift_score", 0)
        set(value) = prefs.edit().putInt("best_drift_score", value).apply()
}
