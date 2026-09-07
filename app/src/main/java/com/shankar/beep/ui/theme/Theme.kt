package com.shankar.beep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Brass,
    onPrimary = OnBrass,
    primaryContainer = BrassWash,
    onPrimaryContainer = Ivory,
    secondary = Ochre,
    onSecondary = OnBrass,
    background = Ink,
    onBackground = Ivory,
    surface = InkRaised,
    onSurface = Ivory,
    surfaceVariant = Panel,
    onSurfaceVariant = IvoryMuted,
    outline = Hairline,
    error = Oxblood,
    onError = Ivory
)

@Composable
fun BeepTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
