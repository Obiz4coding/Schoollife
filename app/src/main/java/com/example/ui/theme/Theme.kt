package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldSecondary,
    onSecondary = OnGoldSecondary,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = TerracottaTertiary,
    onTertiary = OnTerracottaTertiary,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = WarmCreamBackground,
    onBackground = InkPrimary,
    surface = WarmPaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = HairlineOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = OnEmeraldDarkPrimary,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = OnEmeraldDarkContainer,
    secondary = GoldDarkSecondary,
    onSecondary = OnGoldDarkSecondary,
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = OnGoldDarkContainer,
    tertiary = TerracottaDarkTertiary,
    onTertiary = OnTerracottaDarkTertiary,
    tertiaryContainer = TerracottaDarkContainer,
    onTertiaryContainer = OnTerracottaDarkContainer,
    background = NightCampusBackground,
    onBackground = NightInkPrimary,
    surface = NightCampusSurface,
    onSurface = NightInkPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightInkMuted,
    outline = NightOutline
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudentLifeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
