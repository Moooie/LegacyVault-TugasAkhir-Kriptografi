package id.upnyk.legacyvault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Skala tipografi ringkas. Judul memakai serif sistem (Noto Serif di Android)
 * supaya terbaca seperti akta/notaris, bukan landing page SaaS.
 * ponytail: font TTF tidak dibundel agar APK tetap kecil; serif & sans dari sistem.
 */
private fun sans(size: Int, weight: FontWeight, line: Int, track: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = track.sp,
)

private fun serif(size: Int, weight: FontWeight, line: Int, track: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = track.sp,
)

val VaultTypography = Typography(
    headlineLarge = serif(27, FontWeight.Bold, 34, -0.4),
    headlineMedium = serif(22, FontWeight.Bold, 29, -0.2),
    titleLarge = serif(18, FontWeight.SemiBold, 25, -0.1),
    titleMedium = sans(15, FontWeight.SemiBold, 21),
    bodyLarge = sans(15, FontWeight.Normal, 24),
    bodyMedium = sans(14, FontWeight.Normal, 21),
    bodySmall = sans(12, FontWeight.Normal, 18),
    labelLarge = sans(13, FontWeight.Medium, 18),
    labelMedium = sans(10, FontWeight.Medium, 14, 1.0),
    labelSmall = sans(10, FontWeight.Medium, 13, 0.6),
)

/** Label kecil berspasi lebar untuk kategori di atas judul. */
val EyebrowStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.4.sp,
)

val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 11.5.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.0.sp,
)
