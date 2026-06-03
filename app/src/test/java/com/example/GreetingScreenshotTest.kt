package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.PlayerStateEntity
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          PlayerIdentityCard(
            player = PlayerStateEntity(
              level = 42,
              exp = 75,
              shadowPoints = 4280,
              strength = 142,
              agility = 128,
              vitality = 92,
              intellect = 78
            ),
            expNeeded = 100
          )
          BentoStatsGrid(
            player = PlayerStateEntity(
              level = 42,
              exp = 75,
              shadowPoints = 4280,
              strength = 142,
              agility = 128,
              vitality = 92,
              intellect = 78
            ),
            onUpgrade = {}
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
