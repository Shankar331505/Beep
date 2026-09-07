package com.shankar.beep.ui.theme

import androidx.compose.ui.graphics.Color

// Warm instrument palette — ink, ivory, brass. No neon, no mint, no violet.

val Ink = Color(0xFF0B0A09)
val InkRaised = Color(0xFF141210)
val Panel = Color(0xFF1A1815)
val PanelRaised = Color(0xFF211E1A)
val Hairline = Color(0xFF2E2A24)
val HairlineStrong = Color(0xFF3A352D)

val Ivory = Color(0xFFF3EEE4)
val IvoryMuted = Color(0xFFB7AFA3)
val Stone = Color(0xFF8A8276)
val Ash = Color(0xFF5C564C)

val Brass = Color(0xFFC4A574)
val BrassDeep = Color(0xFFA68654)
val BrassWash = Color(0xFF2A2418)
val OnBrass = Color(0xFF1A140C)

val Oxblood = Color(0xFFC45C4A)
val OxbloodWash = Color(0xFF2A1612)

val Ochre = Color(0xFFC9A15B)
val OchreWash = Color(0xFF2A2214)

// Semantic aliases used across the app
val BgDark = Ink
val SurfaceDark = InkRaised
val CardDark = Panel
val CardDarkElevated = PanelRaised
val CardBorderDark = Hairline

val AccentPrimary = Brass
val AccentPressed = BrassDeep
val AccentSoftBg = BrassWash
val AccentBorder = Brass

val AccentEmergencyRed = Oxblood
val EmergencySoftBg = OxbloodWash
val AccentAmber = Ochre
val AmberSoftBg = OchreWash

val TextWhite = Ivory
val TextMutedGrey = IvoryMuted
val TextSubtle = Stone
val TextOnMint = OnBrass
val TextOnAccent = OnBrass

// Legacy aliases so existing call sites keep compiling during the redesign
val DarkBackground = Ink
val DarkSurface = InkRaised
val DarkCard = Panel
val DarkCardBorder = Hairline
val PrimaryBlue = Brass
val PrimaryIndigo = Brass
val AccentCyan = Brass
val EmergencyCoral = Oxblood
val EmergencyRedBg = OxbloodWash
val AmberWarning = Ochre
val AmberBg = OchreWash
val EmeraldSuccess = Brass
val EmeraldBg = BrassWash
val MintPrimary = Brass
val MintPressed = BrassDeep
val MintSoftBg = BrassWash
val MintBorder = Brass
val TextPrimary = Ivory
val TextSecondary = IvoryMuted
val TextMuted = Stone
