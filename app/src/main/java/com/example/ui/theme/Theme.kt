package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestPine,
    onPrimary = Color.White,
    primaryContainer = ForestPineContainer,
    onPrimaryContainer = OnForestPineContainer,
    secondary = EmberTerracotta,
    onSecondary = Color.White,
    secondaryContainer = EmberContainer,
    onSecondaryContainer = OnEmberContainer,
    tertiary = GoldenOchre,
    onTertiary = Color.White,
    tertiaryContainer = OchreContainer,
    onTertiaryContainer = OnOchreContainer,
    background = WarmStoneBg,
    onBackground = TrailInk,
    surface = WarmStoneSurface,
    onSurface = TrailInk,
    surfaceVariant = WarmStoneContainer,
    onSurfaceVariant = TrailMuted,
    surfaceContainerLow = WarmStoneSurface,
    surfaceContainer = WarmStoneContainer,
    surfaceContainerHigh = WarmStoneHigh,
    outline = TrailOutline,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = SageMoss,
    onPrimary = OnSageMoss,
    primaryContainer = SageMossContainer,
    onPrimaryContainer = ForestPineContainer,
    secondary = WarmEmberLight,
    onSecondary = OnWarmEmber,
    secondaryContainer = EmberDarkContainer,
    onSecondaryContainer = EmberContainer,
    tertiary = AmberLantern,
    onTertiary = OnOchreContainer,
    tertiaryContainer = OnOchreContainer,
    onTertiaryContainer = OchreContainer,
    background = NightPineBg,
    onBackground = NightInk,
    surface = NightPineSurface,
    onSurface = NightInk,
    surfaceVariant = NightPineContainer,
    onSurfaceVariant = NightMuted,
    surfaceContainerLow = NightPineSurface,
    surfaceContainer = NightPineContainer,
    surfaceContainerHigh = NightPineHigh,
    outline = NightOutline,
    error = ErrorRedDark,
    onError = Color(0xFF690005)
)

val CampKitShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CampKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CampKitTypography,
        shapes = CampKitShapes,
        content = content
    )
}
