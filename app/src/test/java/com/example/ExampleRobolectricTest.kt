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
  }
}
