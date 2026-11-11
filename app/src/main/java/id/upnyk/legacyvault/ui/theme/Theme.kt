package id.upnyk.legacyvault.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Tema tunggal: terang. Tidak ada varian gelap agar warna status tetap konsisten saat demo. */
private val VaultColorScheme = lightColorScheme(
    primary = VaultBrand,
    onPrimary = VaultOnBrand,
    primaryContainer = VaultPrimaryContainer,
    onPrimaryContainer = VaultOnPrimaryContainer,
    secondary = VaultSecondary,
    onSecondary = VaultOnSecondary,
    secondaryContainer = VaultSecondaryContainer,
    onSecondaryContainer = VaultOnSecondaryContainer,
    tertiary = VaultWarning,
    onTertiary = VaultOnPrimary,
    background = VaultBackground,
    onBackground = VaultText,
    surface = VaultSurface,
    onSurface = VaultText,
    surfaceVariant = VaultSurfaceVariant,
    onSurfaceVariant = VaultTextMuted,
    surfaceContainerLowest = VaultSurface,
    surfaceContainerLow = VaultRaised,
    surfaceContainer = VaultSurfaceVariant,
    surfaceContainerHigh = VaultSurfaceVariant,
    surfaceContainerHighest = VaultRaised,
    outline = VaultOutline,
    outlineVariant = VaultHairline,
    error = VaultError,
    onError = VaultOnPrimary,
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFBE4E1),
    onErrorContainer = VaultError,
)

@Composable
fun LegacyVaultTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = VaultColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = VaultTypography,
        content = content,
    )
}
