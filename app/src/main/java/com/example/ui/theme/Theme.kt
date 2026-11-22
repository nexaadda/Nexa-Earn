package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NexaEarnColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldSoft,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberGold,
    onTertiary = AmberOnGold,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = TextPrimary,
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = MidnightCard,
    surfaceContainerHigh = MidnightCardHigh,
    outline = MidnightOutline,
    error = CoralError,
    onError = TextPrimary,
    errorContainer = CoralContainer,
    onErrorContainer = CoralSoft
)

private val NexaEarnShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NexaEarnTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexaEarnColorScheme,
        typography = NexaEarnTypography,
        shapes = NexaEarnShapes,
        content = content
    )
}
