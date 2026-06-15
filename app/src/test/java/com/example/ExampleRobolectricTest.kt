package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.theme.MyApplicationTheme

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Tessera", appName)
    }

    @Test
    fun `render app`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                TesseraApp()
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `render bottom nav bar`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070909))
                        .padding(20.dp),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    BottomNavBar(
                        isExpanded = false,
                        onExpandedChange = {},
                        onHoveredItemChange = {},
                        currentRoute = "home",
                        onNavigate = {},
                        onCameraClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("app/src/test/screenshots/bottom_nav_bar.png")
    }
}
