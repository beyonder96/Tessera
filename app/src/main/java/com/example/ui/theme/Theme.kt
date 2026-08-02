package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppTheme = staticCompositionLocalOf { "dark" }

private val DeepBlackColorScheme = darkColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryGold,
    tertiary = TertiaryPurple,
    background = BackgroundDeepBlack,
    surface = SurfaceDeepBlack,
    onPrimary = BackgroundDeepBlack,
    onSecondary = BackgroundDeepBlack,
    onTertiary = BackgroundDeepBlack,
    onBackground = OnBackgroundDeepBlack,
    onSurface = OnSurfaceDeepBlack,
    surfaceVariant = SurfaceVariantDeepBlack,
    outline = BorderDeepBlack
)

private val PureWhiteColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    secondary = Color(0xFFD97706),
    tertiary = TertiaryPurple,
    background = BackgroundPureWhite,
    surface = SurfacePureWhite,
    onPrimary = BackgroundPureWhite,
    onSecondary = BackgroundPureWhite,
    onTertiary = BackgroundPureWhite,
    onBackground = OnBackgroundPureWhite,
    onSurface = OnSurfacePureWhite,
    surfaceVariant = SurfaceVariantPureWhite,
    outline = BorderPureWhite
)

@Composable
fun MyApplicationTheme(
    appTheme: String = "dark",
    darkTheme: Boolean = appTheme == "dark",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DeepBlackColorScheme else PureWhiteColorScheme

    CompositionLocalProvider(LocalAppTheme provides if (darkTheme) "dark" else "light") {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
