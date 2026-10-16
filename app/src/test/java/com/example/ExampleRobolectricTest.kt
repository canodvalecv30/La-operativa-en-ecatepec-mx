package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine.CarCatalog
import com.example.game.engine.CarPhysics
import com.example.game.engine.CharacterPhysics
import com.example.game.engine.GameEngine
import com.example.game.engine.GameMode
import com.example.game.engine.GamePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Apex Drive 3D", appName)
  }

  @Test
  fun `test car catalog and starter car`() {
    assertTrue(CarCatalog.CARS.isNotEmpty())
    val starter = CarCatalog.CARS[0]
    assertEquals("apex_gt", starter.id)
    assertEquals(0, starter.price)
  }

  @Test
  fun `test car physics acceleration and braking`() {
    val starter = CarCatalog.CARS[0]
    val physics = CarPhysics(starter)
    assertEquals(0f, physics.speedKmH, 0.01f)

    // Accelerate with full throttle
    physics.update(throttle = 1.0f, brake = 0f, steer = 0f, handbrake = false, nitroRequested = false, dt = 0.5f)
    assertTrue("Car should accelerate with throttle", physics.speedKmH > 0f)

    // Brake to decelerate
    val currentSpeed = physics.speedKmH
    physics.update(throttle = 0f, brake = 1.0f, steer = 0f, handbrake = false, nitroRequested = false, dt = 0.5f)
    assertTrue("Car should decelerate with brake", physics.speedKmH < currentSpeed)
  }

  @Test
  fun `test character on foot movement and vehicle exit enter`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = GamePreferences(context)
    val engine = GameEngine(context, prefs, GameMode.FREE_ROAM)

    assertFalse("Should start driving inside car", engine.isOnFoot)

    // Exit vehicle
    engine.exitVehicle()
    assertTrue("Should now be on foot outside car", engine.isOnFoot)
    assertTrue("Character should be near car door", engine.canEnterCar)

    // Move character away from car
    val char = engine.characterPhysics
    char.update(inputX = 10f, inputZ = 10f, sprint = true, dt = 1.0f)
    assertTrue("Character should move", char.isMoving)

    // Jump character
    char.jump()
    assertTrue("Character should be jumping", char.isJumping)

    // Return to car and re-enter
    char.posX = engine.carPhysics.posX + 1.0f
    char.posZ = engine.carPhysics.posZ
    assertTrue("Should be able to re-enter when near", char.canEnterCar(engine.carPhysics))

    engine.enterVehicle()
    assertFalse("Should be back in car", engine.isOnFoot)

    engine.destroy()
  }

  @Test
  fun `test game preferences defaults`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = GamePreferences(context)
    assertTrue("Starter car must be unlocked by default", prefs.isCarUnlocked("apex_gt"))
    assertTrue("Player should start with initial coins", prefs.coins >= 1000)
    assertEquals(com.example.game.engine.GraphicsQuality.HIGH, prefs.graphicsQuality)
    prefs.graphicsQuality = com.example.game.engine.GraphicsQuality.ULTRA
    assertEquals(com.example.game.engine.GraphicsQuality.ULTRA, prefs.graphicsQuality)
    prefs.graphicsQuality = com.example.game.engine.GraphicsQuality.EXTREME
    assertEquals(com.example.game.engine.GraphicsQuality.EXTREME, prefs.graphicsQuality)

    // Character / Pilot skin default and persistence
    assertEquals(com.example.game.engine.CharacterSkin.PRO_RACER, prefs.characterSkin)
    prefs.characterSkin = com.example.game.engine.CharacterSkin.CYBER_PILOT
    assertEquals(com.example.game.engine.CharacterSkin.CYBER_PILOT, prefs.characterSkin)

    // Optimization toggles
    assertTrue(prefs.roadReflectionsEnabled)
    assertTrue(prefs.bloomEnabled)
  }

  @Test
  fun `test car customization colors decals and rims`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = GamePreferences(context)
    val carId = "apex_gt"

    // Default color & custom paint color
    val defaultColor = prefs.getCarColor(carId, 0xFFE50914)
    assertEquals(0xFFE50914, defaultColor)
    prefs.setCarColor(carId, 0xFF00E5FF)
    assertEquals(0xFF00E5FF, prefs.getCarColor(carId, 0xFFE50914))

    // Paint finish
    assertEquals(com.example.game.engine.PaintFinish.METALLIC, prefs.getCarFinish(carId))
    prefs.setCarFinish(carId, com.example.game.engine.PaintFinish.CHROME)
    assertEquals(com.example.game.engine.PaintFinish.CHROME, prefs.getCarFinish(carId))

    // Decals: default NONE is unlocked
    assertEquals(com.example.game.engine.DecalType.NONE, prefs.getCarDecal(carId))
    assertTrue(prefs.isDecalUnlocked(carId, com.example.game.engine.DecalType.NONE))

    // Lock & unlock custom decal
    assertFalse(prefs.isDecalUnlocked(carId, com.example.game.engine.DecalType.RACING_STRIPES))
    prefs.unlockDecal(carId, com.example.game.engine.DecalType.RACING_STRIPES)
    assertTrue(prefs.isDecalUnlocked(carId, com.example.game.engine.DecalType.RACING_STRIPES))

    prefs.setCarDecal(carId, com.example.game.engine.DecalType.RACING_STRIPES)
    assertEquals(com.example.game.engine.DecalType.RACING_STRIPES, prefs.getCarDecal(carId))

    // Decal color
    prefs.setCarDecalColor(carId, 0xFFFFD600)
    assertEquals(0xFFFFD600, prefs.getCarDecalColor(carId))

    // Wheel rims & brake calipers
    prefs.setCarRimColor(carId, 0xFFFFD700)
    assertEquals(0xFFFFD700, prefs.getCarRimColor(carId))

    prefs.setCarCaliperColor(carId, 0xFF00E5FF)
    assertEquals(0xFF00E5FF, prefs.getCarCaliperColor(carId))
  }

  @Test
  fun `test game map selection and persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = GamePreferences(context)

    assertEquals(com.example.game.engine.GameMap.NEON_CITY, prefs.selectedMap)
    prefs.selectedMap = com.example.game.engine.GameMap.COASTAL_SUNSET
    assertEquals(com.example.game.engine.GameMap.COASTAL_SUNSET, prefs.selectedMap)
    prefs.selectedMap = com.example.game.engine.GameMap.STUNT_DISTRICT
    assertEquals(com.example.game.engine.GameMap.STUNT_DISTRICT, prefs.selectedMap)
  }

  @Test
  fun `test track world cross streets and pedestrians generation`() {
    val world = com.example.game.engine.TrackWorld(
        mode = GameMode.FREE_ROAM,
        map = com.example.game.engine.GameMap.NEON_CITY
    )

    // Cross-streets verification
    assertTrue("Should generate cross-streets", world.crossStreets.isNotEmpty())
    val firstCross = world.crossStreets[0]
    assertTrue("Cross-street has valid Z position", firstCross.posZ > 0f)
    assertTrue("Cross-street has valid name", firstCross.name.isNotBlank())

    // Pedestrians verification
    assertTrue("Should generate pedestrians on sidewalks and crosswalks", world.pedestrians.isNotEmpty())
    val firstPed = world.pedestrians[0]
    assertTrue("Pedestrian has positive speed", firstPed.speed > 0f)
    assertTrue("Pedestrian is spawned along sidewalk or crosswalk", kotlin.math.abs(firstPed.posX) > 5f)

    // Pedestrian evasion test
    val starter = CarCatalog.CARS[0]
    val car = CarPhysics(starter)
    car.speedKmH = 80f
    car.posX = firstPed.posX
    car.posZ = firstPed.posZ - 2f

    val notice = world.update(playerCar = car, playerChar = null, isOnFoot = false, dt = 0.05f)
    assertEquals(com.example.game.engine.PedestrianState.DODGING, firstPed.state)
    assertTrue(world.pedestrianDodgesCount > 0)
  }

  @Test
  fun `test dynamic map switching in game engine`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = GamePreferences(context)
    val engine = GameEngine(context, prefs, GameMode.FREE_ROAM)

    assertEquals(com.example.game.engine.GameMap.NEON_CITY, engine.activeMap)
    engine.changeMap(com.example.game.engine.GameMap.COASTAL_SUNSET)
    assertEquals(com.example.game.engine.GameMap.COASTAL_SUNSET, engine.activeMap)
    assertEquals(com.example.game.engine.GameMap.COASTAL_SUNSET, prefs.selectedMap)
    assertTrue(engine.trackWorld.crossStreets.isNotEmpty())
    assertTrue(engine.trackWorld.pedestrians.isNotEmpty())

    engine.destroy()
  }
}

