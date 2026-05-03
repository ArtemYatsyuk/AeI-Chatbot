package com.aei.chatbot.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aei.chatbot.R

val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

// ── DM Sans — closest freely-available match to Claude.ai's Söhne typeface ─────
// Söhne by Klim Type Foundry is not publicly available; DM Sans shares the same
// geometric humanist proportions, optical weight, and reading comfort.
private val DMSansFamily = FontFamily(
    Font(GoogleFont("DM Sans"), GoogleFontsProvider, FontWeight.Light),
    Font(GoogleFont("DM Sans"), GoogleFontsProvider, FontWeight.Normal),
    Font(GoogleFont("DM Sans"), GoogleFontsProvider, FontWeight.Medium),
    Font(GoogleFont("DM Sans"), GoogleFontsProvider, FontWeight.SemiBold),
    Font(GoogleFont("DM Sans"), GoogleFontsProvider, FontWeight.Bold),
    Font(GoogleFont("DM Sans"), GoogleFontsProvider, FontWeight.ExtraBold),
)

val AeIShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val AeITypography = Typography(
    headlineLarge  = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Bold,    fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Bold,    fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall  = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.SemiBold,fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge     = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.SemiBold,fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium    = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.SemiBold,fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall     = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodyLarge      = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Normal,  fontSize = 15.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
    bodyMedium     = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.sp),
    bodySmall      = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    labelLarge     = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    labelMedium    = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall     = TextStyle(fontFamily = DMSansFamily, fontWeight = FontWeight.Medium,  fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp),
)

fun fontSizeMultiplier(fontSize: String): Float = when (fontSize) {
    "small" -> 0.88f
    "large" -> 1.12f
    "xl"    -> 1.25f
    else    -> 1.0f
}