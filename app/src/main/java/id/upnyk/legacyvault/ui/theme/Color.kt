package id.upnyk.legacyvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet "Krem & Hijau Pinus" — dokumen/akta, terang.
 *
 * Aturan pemakaian:
 * - VaultBrand (hijau pinus) HANYA untuk eyebrow, item nav aktif, dan focus ring.
 * - Aksi utama memakai VaultPrimary (tinta gelap). Satu tombol terisi per layar.
 * - VaultOk / VaultWarning / VaultError HANYA untuk status, tidak pernah untuk CTA.
 *
 * Kontras teks terhadap VaultBackground:
 *   VaultText 15.9:1 · VaultTextMuted 6.4:1 · VaultTextFaint 4.6:1
 */
val VaultBrand = Color(0xFF0E5C4A)
val VaultBrandDim = Color(0xFF7FA99A)
val VaultOnBrand = Color(0xFFFFFFFF)

val VaultPrimary = Color(0xFF16211D)
val VaultOnPrimary = Color(0xFFFFFFFF)
val VaultPrimaryContainer = Color(0xFFE1EEE9)
val VaultOnPrimaryContainer = Color(0xFF0B3F32)

val VaultSecondary = Color(0xFF545C67)
val VaultOnSecondary = Color(0xFFFFFFFF)
val VaultSecondaryContainer = Color(0xFFEDE9DF)
val VaultOnSecondaryContainer = Color(0xFF2A2F36)

val VaultOk = Color(0xFF1F7A4D)
val VaultWarning = Color(0xFFB26B18)
val VaultError = Color(0xFFB8342B)
val VaultInfo = Color(0xFF1D5AA6)

// Nama lama dipertahankan agar seluruh layar tetap kompilasi tanpa disentuh.
val VaultAccent = VaultOk
val VaultAccentDim = Color(0xFFBFD9CC)
val VaultOnAccent = Color(0xFFFFFFFF)

val VaultBackground = Color(0xFFF6F4EF)
val VaultSurface = Color(0xFFFFFFFF)
val VaultSurfaceVariant = Color(0xFFF1EEE6)
val VaultRaised = Color(0xFFFBF9F4)
val VaultOutline = Color(0xFFD8D2C4)
val VaultHairline = Color(0xFFE3DED2)

val VaultText = Color(0xFF14171C)
val VaultTextMuted = Color(0xFF545C67)
val VaultTextFaint = Color(0xFF6E7681)

val VaultBorder = Color(0xFFE3DED2)
