package id.upnyk.legacyvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.ui.theme.EyebrowStyle
import id.upnyk.legacyvault.ui.theme.MonoStyle
import id.upnyk.legacyvault.ui.theme.VaultBrand
import id.upnyk.legacyvault.ui.theme.VaultHairline
import id.upnyk.legacyvault.ui.theme.VaultSurface
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted

/** Satu baris pada panel X-Ray: label proses + nilai antara/ciphertext. */
data class InspectorRow(
    val label: String,
    val value: String,
    val tone: PillTone = PillTone.NEUTRAL,
)

/** Rekaman langkah kriptografi yang ditampilkan ke penguji. */
data class CipherTrace(
    val criterion: String,
    val title: String,
    val algorithm: String,
    val rows: List<InspectorRow>,
    val verdict: String,
    val verdictTone: PillTone = PillTone.OK,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoInspectorSheet(
    trace: CipherTrace?,
    onDismiss: () -> Unit,
) {
    if (trace == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VaultSurface,
        contentColor = VaultText,
        scrimColor = Color.Black.copy(alpha = 0.70f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VaultSpacing.md)
                .padding(bottom = VaultSpacing.xl),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("CRYPTO INSPECTOR", style = EyebrowStyle, color = VaultBrand)
                    Spacer(Modifier.height(4.dp))
                    Text(trace.title, style = MaterialTheme.typography.headlineMedium, color = VaultText)
                    Text(trace.criterion, style = MaterialTheme.typography.bodySmall, color = VaultTextFaint)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup panel X-Ray", tint = VaultTextMuted)
                }
            }

            Spacer(Modifier.height(VaultSpacing.sm))
            VerdictBox(trace.verdict, trace.verdictTone)

            Spacer(Modifier.height(VaultSpacing.md))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VaultSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, VaultHairline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text("ALGORITMA", style = EyebrowStyle, color = VaultTextFaint)
                Spacer(Modifier.width(10.dp))
                Text(trace.algorithm, style = MonoStyle, color = VaultTextMuted, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(VaultSpacing.sm))
            trace.rows.forEachIndexed { index, row ->
                InspectorRowView(row)
                if (index != trace.rows.lastIndex) Hairline()
            }
        }
    }
}

@Composable
private fun InspectorRowView(row: InspectorRow) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.label.uppercase(),
                style = EyebrowStyle,
                color = VaultTextFaint,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .background(VaultSurface, RoundedCornerShape(5.dp))
                    .border(1.dp, VaultHairline, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clickable { clipboard.setText(AnnotatedString(row.value)) },
            ) {
                Text("salin", style = MaterialTheme.typography.labelSmall, color = VaultTextMuted)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            row.value.ifBlank { "-" },
            style = MonoStyle,
            color = if (row.tone == PillTone.NEUTRAL) VaultText else row.tone.color(),
        )
    }
}

@Composable
private fun VerdictBox(text: String, tone: PillTone) {
    val color = tone.color()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(6.dp).background(color, RoundedCornerShape(999.dp)))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

/** Tombol kecil pembuka panel X-Ray. Semua hash tinggal di dalam panel ini. */
@Composable
fun InspectorTrigger(
    onClick: () -> Unit,
    label: String = "Lihat detail kripto",
    modifier: Modifier = Modifier,
) {
    VaultOutlineButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        icon = Icons.Filled.Info,
    )
}
