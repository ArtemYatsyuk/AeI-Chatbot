package com.aei.chatbot.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class AeIColors(
    val userBubble: Color,
    val aiBubble: Color,
    val inputBackground: Color,
    val claudeOrange: Color,
    val outline: Color
)

val LocalAeIColors = staticCompositionLocalOf {
    AeIColors(
        userBubble     = DarkUserBubble,
        aiBubble       = DarkAiBubble,
        inputBackground= DarkInputBackground,
        claudeOrange   = DarkPrimary,
        outline        = DarkOutline
    )
}

private val DarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = Color(0xFF1C1410),
    primaryContainer     = Color(0xFF3D2418),
    onPrimaryContainer   = Color(0xFFFFBBA0),
    secondary            = DarkSecondary,
    onSecondary          = Color(0xFF1C1917),
    secondaryContainer   = Color(0xFF2D2535),
    onSecondaryContainer = Color(0xFFD4C8E8),
    tertiary             = Color(0xFF9E9E9E),
    tertiaryContainer    = Color(0xFF2A2828),
    onTertiaryContainer  = Color(0xFFDDDAD4),
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = Color(0xFFB0ABA3),
    outline              = DarkOutline,
    outlineVariant       = Color(0xFF3A3733),
    error                = DarkError,
    onError              = Color.White,
    errorContainer       = Color(0xFF3D1818),
    onErrorContainer     = Color(0xFFFFB3B3),
    inverseSurface       = Color(0xFFF0EDE8),
    inverseOnSurface     = Color(0xFF1C1917),
    inversePrimary       = LightPrimary,
    scrim                = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFFFDDD4),
    onPrimaryContainer   = Color(0xFF3D1408),
    secondary            = LightSecondary,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFECE6F4),
    onSecondaryContainer = Color(0xFF1A0E2E),
    tertiary             = Color(0xFF6B6B6B),
    tertiaryContainer    = Color(0xFFF0EDE8),
    onTertiaryContainer  = Color(0xFF1A1714),
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = Color(0xFF6B6560),
    outline              = LightOutline,
    outlineVariant       = Color(0xFFECE8E2),
    error                = Color(0xFFC0392B),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFEBE8),
    onErrorContainer     = Color(0xFF5C1010),
    inverseSurface       = Color(0xFF2D2B29),
    inverseOnSurface     = Color(0xFFF0EDE8),
    inversePrimary       = DarkPrimary,
    scrim                = Color(0xFF000000),
)

@Composable
fun AeITheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false,   // off by default — preserves claude.ai warmth
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark"  -> true
        "light" -> false
        else    -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (isDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        isDark -> DarkColorScheme
        else   -> LightColorScheme
    }

    val aeIColors = if (isDark) AeIColors(
        userBubble      = DarkUserBubble,
        aiBubble        = DarkAiBubble,
        inputBackground = DarkInputBackground,
        claudeOrange    = DarkPrimary,
        outline         = DarkOutline
    ) else AeIColors(
        userBubble      = LightUserBubble,
        aiBubble        = LightAiBubble,
        inputBackground = LightInputBackground,
        claudeOrange    = LightPrimary,
        outline         = LightOutline
    )

    CompositionLocalProvider(LocalAeIColors provides aeIColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AeITypography,
            shapes      = AeIShapes,
            content     = content
        )
    }
}