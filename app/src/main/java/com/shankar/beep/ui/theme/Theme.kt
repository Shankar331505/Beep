package com.shankar.beep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.shankar.beep.data.ThemeChoice
import com.shankar.beep.detect.Category

/*
 * "Beacon" — Beep's design language.
 * A calm, near-black canvas so alerts can be loud. One warm signal colour
 * (beacon orange) for "Beep is on", and one clear hue per kind of sound,
 * reused for the icon, the vibration's notification colour and the alert page.
 */

val Beacon = Color(0xFFFF7A45)
val BeaconDeep = Color(0xFFE85A24)

val SafetyRed = Color(0xFFFF4D5E)
val PeopleViolet = Color(0xFFA78BFA)
val HomeTeal = Color(0xFF2EC4A6)
val StreetAmber = Color(0xFFFFB627)
val CustomBlue = Color(0xFF5AA9FF)

fun categoryColor(category: Category): Color = when (category) {
    Category.SAFETY -> SafetyRed
    Category.PEOPLE -> PeopleViolet
    Category.HOME -> HomeTeal
    Category.STREET -> StreetAmber
    Category.CUSTOM -> CustomBlue
}

fun categoryArgb(category: Category): Int = categoryColor(category).toArgb()

@Immutable
data class BeepPalette(
    val canvas: Color,
    val card: Color,
    val cardHigh: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val signal: Color,
    val onSignal: Color,
    val isDark: Boolean,
)

private val DarkPalette = BeepPalette(
    canvas = Color(0xFF0B0C0F),
    card = Color(0xFF15171C),
    cardHigh = Color(0xFF1E2128),
    line = Color(0xFF272B33),
    text = Color(0xFFF1F2F5),
    muted = Color(0xFF9AA1AE),
    faint = Color(0xFF5D6471),
    signal = Beacon,
    onSignal = Color(0xFF1A0B04),
    isDark = true,
)

private val LightPalette = BeepPalette(
    canvas = Color(0xFFF5F4F1),
    card = Color(0xFFFFFFFF),
    cardHigh = Color(0xFFEDEBE6),
    line = Color(0xFFE0DDD6),
    text = Color(0xFF15171B),
    muted = Color(0xFF5E6470),
    faint = Color(0xFF9CA1AB),
    signal = BeaconDeep,
    onSignal = Color.White,
    isDark = false,
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

object Beep {
    val palette: BeepPalette @Composable get() = LocalPalette.current
}

private val Sans = FontFamily.SansSerif

private val BeepTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-0.03).em),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 44.sp, letterSpacing = (-0.02).em),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-0.02).em, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.01).em, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.01.em),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.02.em),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.08.em),
)

@Composable
fun BeepTheme(choice: ThemeChoice = ThemeChoice.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.DARK -> true
        ThemeChoice.LIGHT -> false
    }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.signal, onPrimary = p.onSignal,
            secondary = PeopleViolet, tertiary = HomeTeal,
            background = p.canvas, onBackground = p.text,
            surface = p.canvas, onSurface = p.text,
            surfaceVariant = p.card, onSurfaceVariant = p.muted,
            surfaceContainer = p.card, surfaceContainerHigh = p.cardHigh,
            surfaceContainerHighest = p.cardHigh, surfaceContainerLow = p.card,
            outline = p.line, outlineVariant = p.line, error = SafetyRed,
        )
    } else {
        lightColorScheme(
            primary = p.signal, onPrimary = p.onSignal,
            secondary = PeopleViolet, tertiary = HomeTeal,
            background = p.canvas, onBackground = p.text,
            surface = p.canvas, onSurface = p.text,
            surfaceVariant = p.card, onSurfaceVariant = p.muted,
            surfaceContainer = p.card, surfaceContainerHigh = p.cardHigh,
            surfaceContainerHighest = p.cardHigh, surfaceContainerLow = p.card,
            outline = p.line, outlineVariant = p.line, error = SafetyRed,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = BeepTypography, content = content)
    }
}
