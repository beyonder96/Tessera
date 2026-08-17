package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.TesseraRepository
import com.example.ui.components.DetailedMatchWidget
import com.example.ui.components.LocalGlassmorphismLevel
import com.example.ui.components.PremiumWeatherWidget
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TesseraViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreensScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getViewModel(): TesseraViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = TesseraRepository(FakeTesseraDao())
        return TesseraViewModel(repository, context)
    }

    @Test
    fun `capture Weather Widget Day`() {
        val dayWeather = TesseraViewModel.WeatherInfo(
            temp = 27.0,
            description = "Céu Limpo",
            city = "Rio de Janeiro",
            weatherCode = 0,
            isDay = true
        )
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CompositionLocalProvider(LocalAppTheme provides "light") {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                        PremiumWeatherWidget(weatherState = dayWeather)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/weather_widget_day.png")
    }

    @Test
    fun `capture Weather Widget Night`() {
        val nightWeather = TesseraViewModel.WeatherInfo(
            temp = 19.0,
            description = "Noite Limpa",
            city = "Rio de Janeiro",
            weatherCode = 0,
            isDay = false
        )
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                CompositionLocalProvider(LocalAppTheme provides "dark") {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                        PremiumWeatherWidget(weatherState = nightWeather)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/weather_widget_night.png")
    }

    @Test
    fun `capture Health Screen Light Mode`() {
        val viewModel = getViewModel()
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                CompositionLocalProvider(LocalAppTheme provides "light", LocalGlassmorphismLevel provides "Blur") {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HealthScreen(viewModel = viewModel)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/health_screen_light.png")
    }

    @Test
    fun `capture Health Screen Dark Mode`() {
        val viewModel = getViewModel()
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                CompositionLocalProvider(LocalAppTheme provides "dark", LocalGlassmorphismLevel provides "Blur") {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HealthScreen(viewModel = viewModel)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/health_screen_dark.png")
    }

    @Test
    fun `capture Football Widget with Standings`() {
        val viewModel = getViewModel()
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                CompositionLocalProvider(LocalAppTheme provides "dark", LocalGlassmorphismLevel provides "Blur") {
                    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                        DetailedMatchWidget(viewModel = viewModel)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/football_widget_scoreboard.png")
    }
}
