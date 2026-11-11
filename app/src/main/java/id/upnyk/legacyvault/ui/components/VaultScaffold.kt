package id.upnyk.legacyvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.ui.theme.VaultBackground
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted

/**
 * Kerangka layar bersama: bilah atas tipis + konten yang dapat digulir.
 * Semua warna berasal dari token tema, bukan hex per layar.
 */
@Composable
fun VaultScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onLock: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onMenu != null) TopBarIcon(Icons.Filled.Menu, "Buka menu", onMenu)
            else if (onBack != null) TopBarIcon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = VaultText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultTextFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (onInfo != null) TopBarIcon(Icons.Filled.Info, "Crypto Inspector", onInfo)
            if (onLock != null) TopBarIcon(Icons.Filled.Lock, "Kunci brankas", onLock)
        }
        Hairline()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VaultSpacing.md)
                .padding(top = VaultSpacing.md, bottom = VaultSpacing.xl),
            content = content,
        )
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = VaultTextMuted, modifier = Modifier.size(19.dp))
    }
}

/** Satu baris aksi di kaki layar: aksi sekunder kiri, aksi utama kanan. */
@Composable
fun ActionRow(
    modifier: Modifier = Modifier,
    primary: @Composable () -> Unit,
    secondary: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (secondary != null) {
            secondary()
            Spacer(Modifier.width(VaultSpacing.sm))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { primary() }
    }
}

/** Judul bagian dengan garis pemisah di atasnya — pengganti kartu bertumpuk. */
@Composable
fun SectionDivider(title: String, trailing: String? = null, top: Int = 24, bottom: Int = 8) {
    Spacer(Modifier.height(top.dp))
    Hairline()
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = bottom.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = id.upnyk.legacyvault.ui.theme.EyebrowStyle,
            color = VaultTextMuted,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                trailing,
                style = id.upnyk.legacyvault.ui.theme.EyebrowStyle,
                color = VaultTextFaint,
            )
        }
    }
    Spacer(Modifier.height(VaultSpacing.sm))
}

@Composable
fun RoundedTag(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}
