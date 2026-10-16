package com.example.game.engine

import android.content.Context
import android.content.SharedPreferences

enum class GameMode(val title: String, val description: String) {
    FREE_ROAM("Modo Libre", "Conduce sin límites por la ciudad y pista de acrobacias."),
    TRAFFIC_RUSH("Tráfico Autopista", "Esquiva tráfico a máxima velocidad. ¡Adelanta al límite!"),
    TIME_ATTACK("Contrarreloj", "Completa vueltas al circuito antes de que acabe el tiempo."),
    DRIFT_MASTER("Maestro del Derrape", "Consigue la máxima puntuación encadenando derrapes.")
}

enum class GameMap(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val fogColor: FloatArray,
    val lightPos: FloatArray,
    val sunColor: FloatArray,
    val roadColor: FloatArray,
    val shoulderColor: FloatArray
) {
    NEON_CITY(
        id = "neon_city",
        title = "Metrópolis Neón",
        subtitle = "Downtown & Rascacielos Nocturnos",
        description = "Avenida metropolitana con rascacielos iluminados, semáforos, pasos de cebra y aceras concurridas.",
        fogColor = floatArrayOf(0.08f, 0.10f, 0.16f),
        lightPos = floatArrayOf(20f, 60f, 40f),
        sunColor = floatArrayOf(1.0f, 0.95f, 0.70f),
        roadColor = floatArrayOf(0.16f, 0.16f, 0.18f),
        shoulderColor = floatArrayOf(0.95f, 0.80f, 0.10f)
    ),
    COASTAL_SUNSET(
        id = "coastal_sunset",
        title = "Paseo Costero & Cañón",
        subtitle = "Puesta de Sol sobre el Océano",
        description = "Paseo marítimo paradisíaco con vista al océano turquesa, playas doradas, palmeras y acantilados al atardecer.",
        fogColor = floatArrayOf(0.38f, 0.22f, 0.20f),
        lightPos = floatArrayOf(35f, 30f, 50f),
        sunColor = floatArrayOf(1.0f, 0.60f, 0.25f),
        roadColor = floatArrayOf(0.22f, 0.22f, 0.24f),
        shoulderColor = floatArrayOf(1.00f, 0.55f, 0.10f)
    ),
    STUNT_DISTRICT(
        id = "stunt_district",
        title = "Distrito Industrial & Rampas",
        subtitle = "Complejo Portuario y Acrobacias",
        description = "Zona portuaria con cruces de contenedores marítimos multicolores, mega rampas acrobáticas, grúas y loopings.",
        fogColor = floatArrayOf(0.14f, 0.14f, 0.18f),
        lightPos = floatArrayOf(15f, 55f, 35f),
        sunColor = floatArrayOf(0.95f, 0.88f, 0.70f),
        roadColor = floatArrayOf(0.18f, 0.18f, 0.20f),
        shoulderColor = floatArrayOf(0.95f, 0.20f, 0.20f)
    )
}

enum class WeaponType(
    val id: String,
    val title: String,
    val subtitle: String,
    val maxAmmo: Int,
    val damage: Int,
    val fireRateSeconds: Float,
    val bulletSpeed: Float,
    val projectileColor: Long,
    val isFullAuto: Boolean
) {
    TACTICAL_PISTOL(
        id = "pistol_9mm",
        title = "Pistola Táctica 9mm",
        subtitle = "Semiautomática precisa y de rápida cadencia",
        maxAmmo = 15,
        damage = 45,
        fireRateSeconds = 0.18f,
        bulletSpeed = 175f,
        projectileColor = 0xFFFFD700,
        isFullAuto = false
    ),
    PLASMA_BLASTER(
        id = "blaster_plasma",
        title = "Blaster de Plasma Cyber",
        subtitle = "Ráfagas ionizadas de alta energía cyan",
        maxAmmo = 24,
        damage = 65,
        fireRateSeconds = 0.13f,
        bulletSpeed = 205f,
        projectileColor = 0xFF00E5FF,
        isFullAuto = true
    ),
    MAGNUM_HEAVY(
        id = "magnum_44",
        title = "Magnum .44 Cañón Pesado",
        subtitle = "Potencia demoledora con proyectiles de tungsteno",
        maxAmmo = 6,
        damage = 130,
        fireRateSeconds = 0.45f,
        bulletSpeed = 240f,
        projectileColor = 0xFFFF3D00,
        isFullAuto = false
    )
}

enum class SteeringType {
    BUTTONS,
    STEERING_WHEEL,
    TILT_SENSOR
}

enum class GraphicsQuality(val title: String, val description: String, val renderDistance: Float, val shadowQuality: Int) {
    LOW("Baja (Fluido)", "Máximo rendimiento para dispositivos modestos", 160f, 0),
    MEDIUM("Media (Equilibrado)", "Balance ideal 60 FPS con efectos", 240f, 1),
    HIGH("Alta (Realista)", "Iluminación metálica, sombras y marcas viales", 300f, 2),
    ULTRA("Ultra (Detalle Extremo)", "Reflejos de asfalto, marcas y partículas densas", 360f, 3),
    EXTREME("Extremo (120 FPS / Realismo)", "Iluminación PBR, reflejos y distancia máxima", 440f, 4)
}

enum class CharacterSkin(
    val id: String,
    val title: String,
    val subtitle: String,
    val suitColorHex: Long,
    val accentColorHex: Long,
    val visorColorHex: Long,
    val helmetColorHex: Long
) {
    PRO_RACER("pro_racer", "Piloto Apex Pro", "Mono ignífugo de fibra, casco aerodinámico y visor cyan", 0xFFE50914, 0xFF12141C, 0xFF00E5FF, 0xFFF5F5F5),
    STREET_DRIFTER("street_drifter", "Drifter Callejero", "Estilo urbano con cazadora deportiva y visera dorada", 0xFF1E212D, 0xFFE50914, 0xFFFFD700, 0xFF2A2D3A),
    CYBER_PILOT("cyber_pilot", "Cyber Rider Neo", "Traje táctico con visor holográfico y neón esmeralda", 0xFF7C4DFF, 0xFF12141C, 0xFF00E676, 0xFF0D0E15),
    GOLDEN_CHAMPION("golden_champ", "Campeón Dorado", "Equipamiento de oro de competición y visor de espejo", 0xFFFFB300, 0xFF212121, 0xFFFFFFFF, 0xFFFFD700)
}

enum class DecalType(val id: String, val title: String, val subtitle: String, val price: Int) {
    NONE("none", "Pintura Limpia", "Sin vinilo, acabado de fábrica", 0),
    RACING_STRIPES("stripes", "Rayas GT", "Doble franja de competición en capó y techo", 300),
    CYBER_SLASH("cyber", "Cyber Slash", "Líneas de velocidad en laterales y puertas", 450),
    CARBON_ROOF("carbon", "Capó & Techo Carbón", "Fibra de carbono negra de contraste", 600),
    RACE_NUMBER("number", "Dorsal #07", "Círculo de circuito profesional en puertas", 400)
}

enum class PaintFinish(val id: String, val title: String, val glossiness: Float) {
    METALLIC("metallic", "Metálico", 1.0f),
    MATTE("matte", "Mate", 0.15f),
    CHROME("chrome", "Cromado", 1.8f),
    NEON("neon", "Neón Glow", 1.3f)
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
        0xFFECEFF1 to "Blanco Perla",
        0xFF00B0FF to "Azul Eléctrico",
        0xFFE91E63 to "Rosa Fucsia",
        0xFF009688 to "Turquesa GT",
        0xFF607D8B to "Gris Titanio"
    )

    val DECAL_COLORS = listOf(
        0xFFFFFFFF to "Blanco Glaciar",
        0xFF12141C to "Negro Ónix",
        0xFFFFD600 to "Amarillo Speed",
        0xFF00E5FF to "Azul Neón",
        0xFFE50914 to "Rojo Fuego",
        0xFF76FF03 to "Verde Ácido",
        0xFFFF9800 to "Naranja F1"
    )

    val RIM_COLORS = listOf(
        0xFFE0E0E0 to "Plata Pulida",
        0xFF1E212D to "Negro Brillante",
        0xFFFFD700 to "Oro Competición",
        0xFF8D6E63 to "Bronce Satinado"
    )

    val CALIPER_COLORS = listOf(
        0xFFE50914 to "Rojo Brembo",
        0xFFFFD600 to "Amarillo Oro",
        0xFF00E5FF to "Azul Eléctrico",
        0xFF76FF03 to "Verde Ácido"
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

    fun getCarDecal(carId: String): DecalType {
        val name = prefs.getString("car_decal_$carId", DecalType.NONE.name)
        return try { DecalType.valueOf(name ?: DecalType.NONE.name) } catch (e: Exception) { DecalType.NONE }
    }

    fun setCarDecal(carId: String, decal: DecalType) {
        prefs.edit().putString("car_decal_$carId", decal.name).apply()
    }

    fun isDecalUnlocked(carId: String, decal: DecalType): Boolean {
        if (decal == DecalType.NONE) return true
        return prefs.getBoolean("decal_unlocked_${carId}_${decal.name}", false)
    }

    fun unlockDecal(carId: String, decal: DecalType) {
        prefs.edit().putBoolean("decal_unlocked_${carId}_${decal.name}", true).apply()
    }

    fun getCarDecalColor(carId: String): Long {
        return prefs.getLong("car_decal_color_$carId", 0xFFFFFFFF)
    }

    fun setCarDecalColor(carId: String, color: Long) {
        prefs.edit().putLong("car_decal_color_$carId", color).apply()
    }

    fun getCarFinish(carId: String): PaintFinish {
        val name = prefs.getString("car_finish_$carId", PaintFinish.METALLIC.name)
        return try { PaintFinish.valueOf(name ?: PaintFinish.METALLIC.name) } catch (e: Exception) { PaintFinish.METALLIC }
    }

    fun setCarFinish(carId: String, finish: PaintFinish) {
        prefs.edit().putString("car_finish_$carId", finish.name).apply()
    }

    fun getCarRimColor(carId: String): Long {
        return prefs.getLong("car_rim_color_$carId", 0xFFE0E0E0)
    }

    fun setCarRimColor(carId: String, color: Long) {
        prefs.edit().putLong("car_rim_color_$carId", color).apply()
    }

    fun getCarCaliperColor(carId: String): Long {
        return prefs.getLong("car_caliper_color_$carId", 0xFFE50914)
    }

    fun setCarCaliperColor(carId: String, color: Long) {
        prefs.edit().putLong("car_caliper_color_$carId", color).apply()
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

    var graphicsQuality: GraphicsQuality
        get() {
            val name = prefs.getString("graphics_quality", GraphicsQuality.HIGH.name)
            return try { GraphicsQuality.valueOf(name ?: GraphicsQuality.HIGH.name) } catch (e: Exception) { GraphicsQuality.HIGH }
        }
        set(value) = prefs.edit().putString("graphics_quality", value.name).apply()

    var bestDistanceRush: Float
        get() = prefs.getFloat("best_distance_rush", 0f)
        set(value) = prefs.edit().putFloat("best_distance_rush", value).apply()

    var bestLapTime: Float
        get() = prefs.getFloat("best_lap_time", 999.9f)
        set(value) = prefs.edit().putFloat("best_lap_time", value).apply()

    var bestDriftScore: Int
        get() = prefs.getInt("best_drift_score", 0)
        set(value) = prefs.edit().putInt("best_drift_score", value).apply()

    var characterSkin: CharacterSkin
        get() {
            val name = prefs.getString("character_skin", CharacterSkin.PRO_RACER.name)
            return try { CharacterSkin.valueOf(name ?: CharacterSkin.PRO_RACER.name) } catch (e: Exception) { CharacterSkin.PRO_RACER }
        }
        set(value) = prefs.edit().putString("character_skin", value.name).apply()

    var roadReflectionsEnabled: Boolean
        get() = prefs.getBoolean("road_reflections_enabled", true)
        set(value) = prefs.edit().putBoolean("road_reflections_enabled", value).apply()

    var bloomEnabled: Boolean
        get() = prefs.getBoolean("bloom_enabled", true)
        set(value) = prefs.edit().putBoolean("bloom_enabled", value).apply()

    var selectedMap: GameMap
        get() {
            val name = prefs.getString("selected_map", GameMap.NEON_CITY.name)
            return try { GameMap.valueOf(name ?: GameMap.NEON_CITY.name) } catch (e: Exception) { GameMap.NEON_CITY }
        }
        set(value) = prefs.edit().putString("selected_map", value.name).apply()

    var selectedWeapon: WeaponType
        get() {
            val name = prefs.getString("selected_weapon", WeaponType.TACTICAL_PISTOL.name)
            return try { WeaponType.valueOf(name ?: WeaponType.TACTICAL_PISTOL.name) } catch (e: Exception) { WeaponType.TACTICAL_PISTOL }
        }
        set(value) = prefs.edit().putString("selected_weapon", value.name).apply()
}
