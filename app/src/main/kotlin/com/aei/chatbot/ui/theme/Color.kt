package com.aei.chatbot.ui.theme

import androidx.compose.ui.graphics.Color

// Dark palette
val DarkBackground = Color(0xFF0D0D0F)
val DarkSurface = Color(0xFF161618)
val DarkSurfaceVariant = Color(0xFF1E1E22)
val DarkPrimary = Color(0xFF7B61FF)
val DarkPrimaryVariant = Color(0xFF9D88FF)
val DarkSecondary = Color(0xFF03DAC6)
val DarkOnBackground = Color(0xFFEAEAEA)
val DarkOnSurface = Color(0xFFD0D0D8)
val DarkError = Color(0xFFFF5252)
val DarkUserBubble = Color(0xFF2A2060)
val DarkAiBubble = Color(0xFF1A1A22)
val DarkInputBackground = Color(0xFF1C1C22)

// Light palette
val LightBackground = Color(0xFFF5F5FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEDF5)
val LightPrimary = Color(0xFF6A50E8)
val LightSecondary = Color(0xFF00C4B4)
val LightOnBackground = Color(0xFF1A1A2E)
val LightOnSurface = Color(0xFF2D2D3A)
val LightUserBubble = Color(0xFFEDE8FF)
val LightAiBubble = Color(0xFFF0F0F8)
val LightInputBackground = Color(0xFFEAEAF5)

// Avatar preset colors
val AvatarViolet = Color(0xFF7B61FF)
val AvatarTeal = Color(0xFF03DAC6)
val AvatarRose = Color(0xFFFF5C8A)
val AvatarAmber = Color(0xFFFFB300)
val AvatarBlue = Color(0xFF2196F3)
val AvatarGreen = Color(0xFF4CAF50)

fun avatarColorFromString(colorName: String): Color = when (colorName) {
    "teal" -> AvatarTeal
    "rose" -> AvatarRose
    "amber" -> AvatarAmber
    "blue" -> AvatarBlue
    "green" -> AvatarGreen
    else -> AvatarViolet
}
