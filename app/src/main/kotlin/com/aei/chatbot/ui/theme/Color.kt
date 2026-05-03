package com.aei.chatbot.ui.theme

import androidx.compose.ui.graphics.Color

// ── Claude.ai-inspired dark palette ──────────────────────────────────────────
// Claude uses warm near-blacks, not cold blue-blacks
val DarkBackground     = Color(0xFF1C1917)   // warm dark (claude sidebar bg)
val DarkSurface        = Color(0xFF242120)   // slightly lighter warm surface
val DarkSurfaceVariant = Color(0xFF2D2B29)   // card/input surface
val DarkPrimary        = Color(0xFFCC785C)   // claude orange-coral accent
val DarkPrimaryVariant = Color(0xFFE09070)   // lighter coral
val DarkSecondary      = Color(0xFF9B8EA8)   // muted purple-lavender
val DarkOnBackground   = Color(0xFFF0EDE8)   // warm white text
val DarkOnSurface      = Color(0xFFCDC8C0)   // secondary warm text
val DarkError          = Color(0xFFE05C5C)   // soft red
val DarkUserBubble     = Color(0xFF2D2B29)   // same as surface variant
val DarkAiBubble       = Color(0xFF1C1917)   // same as bg (no bubble)
val DarkInputBackground= Color(0xFF2D2B29)
val DarkOutline        = Color(0xFF3D3A37)

// ── Claude.ai-inspired light palette ─────────────────────────────────────────
val LightBackground     = Color(0xFFFAF9F7)  // warm off-white (claude main bg)
val LightSurface        = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF2EFE8)  // warm grey for inputs
val LightPrimary        = Color(0xFFBF6750)  // claude coral/orange
val LightSecondary      = Color(0xFF8B7FA8)  // muted purple
val LightOnBackground   = Color(0xFF1A1714)  // very dark warm text
val LightOnSurface      = Color(0xFF403C38)  // secondary warm text
val LightUserBubble     = Color(0xFFF0EDE8)  // light warm for user messages
val LightAiBubble       = Color(0xFFFAF9F7)  // same as bg
val LightInputBackground= Color(0xFFF2EFE8)
val LightOutline        = Color(0xFFDDDAD4)

// ── Avatar presets ────────────────────────────────────────────────────────────
val AvatarCoral  = Color(0xFFCC785C)
val AvatarSage   = Color(0xFF7A9E87)
val AvatarSlate  = Color(0xFF7B8FA8)
val AvatarAmber  = Color(0xFFD4A853)
val AvatarRose   = Color(0xFFB87B8E)
val AvatarTeal   = Color(0xFF5B9E9A)

fun avatarColorFromString(colorName: String): Color = when (colorName) {
    "teal"   -> AvatarTeal
    "rose"   -> AvatarRose
    "amber"  -> AvatarAmber
    "slate"  -> AvatarSlate
    "sage"   -> AvatarSage
    else     -> AvatarCoral
}