package id.upnyk.legacyvault.ui.screens.stegano

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.core.database.entity.StegoImageEntity
import id.upnyk.legacyvault.ui.VaultViewModel
import id.upnyk.legacyvault.ui.components.Disclosure
import id.upnyk.legacyvault.ui.components.Eyebrow
import id.upnyk.legacyvault.ui.components.Hairline
import id.upnyk.legacyvault.ui.components.InfoLine
import id.upnyk.legacyvault.ui.components.MetricGrid
import id.upnyk.legacyvault.ui.components.MonoBlock
import id.upnyk.legacyvault.ui.components.PillTone
import id.upnyk.legacyvault.ui.components.SectionDivider
import id.upnyk.legacyvault.ui.components.StatusPill
import id.upnyk.legacyvault.ui.components.VaultButton
import id.upnyk.legacyvault.ui.components.VaultOutlineButton
import id.upnyk.legacyvault.ui.components.VaultScaffold
import id.upnyk.legacyvault.ui.components.VaultSpacing
import id.upnyk.legacyvault.ui.components.VaultTextField
import id.upnyk.legacyvault.ui.theme.VaultAccent
import id.upnyk.legacyvault.ui.theme.VaultSurfaceVariant
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Kriteria 6 — Galeri Memori Keluarga: LSB 24-bit + PRNG seed, split view cover/stego + PSNR. */
@Composable
fun SteganoScreen(vm: VaultViewModel, onMenu: () -> Unit) {
    val state = vm.stego
    val items by vm.stegoImages.collectAsState()

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::onCoverPicked)
    }

    VaultScaffold(
        title = "Galeri Memori",
        subtitle = "Kriteria 6 · LSB + PRNG seed",
        onMenu = onMenu,
        onLock = vm::lockVault,
        onInfo = vm::reopenTrace,
    ) {
        if (state.extractTarget == null) {
            Eyebrow("Steganografi spatial domain")
            Spacer(Modifier.height(6.dp))
            Text("Sisipkan pesan ke foto", style = MaterialTheme.typography.headlineLarge, color = VaultText)
            Spacer(Modifier.height(3.dp))
            Text(
                "Petunjuk lokasi harta disisipkan pada bit ke-0 kanal R, G, B dengan urutan piksel acak.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextMuted,
            )

            Spacer(Modifier.height(VaultSpacing.lg))
            VaultOutlineButton(
                text = if (state.coverName.isBlank()) "Pilih foto cover" else state.coverName,
                onClick = { pickCover.launch(arrayOf("image/*")) },
                modifier = Modifier.fillMaxWidth(),
            )
            state.coverBitmap?.let { bitmap ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "${bitmap.width} × ${bitmap.height} px · kapasitas ${bitmap.width * bitmap.height * 3} bit",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextFaint,
                )
            }

            Spacer(Modifier.height(VaultSpacing.md))
            VaultTextField(
                value = state.title,
                onValueChange = vm::onStegoTitle,
                label = "Judul memori",
                placeholder = "Foto rumah Jogja 1998",
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultTextField(
                value = state.secretMessage,
                onValueChange = vm::onStegoMessage,
                label = "Pesan rahasia",
                singleLine = false,
                minLines = 3,
                placeholder = "Sertifikat tanah di lemari buku lantai 2…",
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultTextField(
                value = state.stegoKey,
                onValueChange = vm::onStegoKey,
                label = "Stego-key (seed PRNG)",
            )

            Spacer(Modifier.height(VaultSpacing.lg))
            VaultButton(
                text = if (state.busy) "Menyisipkan…" else "Sisipkan & simpan PNG",
                onClick = vm::embedStego,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.message.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.sm))
                InfoLine(state.message, if (state.isError) PillTone.ERROR else PillTone.OK)
            }

            if (state.coverBitmap != null && state.resultBitmap != null) {
                Spacer(Modifier.height(VaultSpacing.lg))
                SectionDivider("Perbandingan citra", "cover vs stego")
                Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
                    SplitPane("COVER", Modifier.weight(1f)) {
                        Image(
                            bitmap = state.coverBitmap!!.asImageBitmap(),
                            contentDescription = "Citra cover asli",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    SplitPane("STEGO", Modifier.weight(1f)) {
                        Image(
                            bitmap = state.resultBitmap!!.asImageBitmap(),
                            contentDescription = "Citra stego hasil sisipan LSB",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                Spacer(Modifier.height(VaultSpacing.md))
                MetricGrid(
                    listOf(
                        Triple("MSE", "%.6f".format(state.mse), VaultText),
                        Triple("PSNR", "%.2f dB".format(state.psnr), VaultAccent),
                        Triple("Payload", "${state.payloadBits} bit", VaultText),
                    ),
                )
            }
        } else {
            Eyebrow("Ekstraksi LSB")
            Spacer(Modifier.height(6.dp))
            Text(
                state.extractTarget!!.title,
                style = MaterialTheme.typography.headlineLarge,
                color = VaultText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            MonoBlock(state.extractTarget!!.stegoImagePath, maxLines = 2)

            Spacer(Modifier.height(VaultSpacing.lg))
            VaultTextField(
                value = state.extractKey,
                onValueChange = vm::onExtractKey,
                label = "Stego-key",
            )
            Spacer(Modifier.height(VaultSpacing.lg))
            VaultButton(
                text = "Ekstrak",
                onClick = vm::extractStego,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultOutlineButton(
                text = "Tutup",
                onClick = vm::cancelStegoExtract,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.extractedText.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.md))
                Text("PESAN TERPULIHKAN", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
                Spacer(Modifier.height(4.dp))
                Text(state.extractedText, style = MaterialTheme.typography.bodyLarge, color = VaultText)
            }
            if (state.message.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.sm))
                InfoLine(state.message, if (state.isError) PillTone.ERROR else PillTone.OK)
            }
        }

        SectionDivider("Galeri stego", "${items.size} gambar")
        if (items.isEmpty()) {
            Text(
                "Belum ada citra stego. Hasil selalu disimpan sebagai PNG agar bit LSB tidak rusak.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        } else {
            items.forEachIndexed { index, item ->
                StegoRow(item, vm)
                if (index != items.lastIndex) Hairline(Modifier.padding(vertical = VaultSpacing.md))
            }
        }

        Spacer(Modifier.height(VaultSpacing.lg))
        Text(
            "32 bit pertama menyimpan panjang pesan (byte), diikuti 8 bit per karakter UTF-8.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextFaint,
        )
    }
}

@Composable
private fun SplitPane(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .background(VaultSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun StegoRow(item: StegoImageEntity, vm: VaultViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                color = VaultText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(item.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        }
        StatusPill("%.1f dB".format(item.psnrScore), PillTone.OK, keepCase = true)
    }

    Disclosure("Detail kripto") {
        Text("PAYLOAD", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
        MonoBlock("${item.payloadBitLength} bit · seed PRNG dari stego-key", maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text("BERKAS STEGO", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
        MonoBlock(item.stegoImagePath, maxLines = 2)
    }

    Spacer(Modifier.height(VaultSpacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        VaultOutlineButton(
            text = "Ekstrak",
            onClick = { vm.startStegoExtract(item) },
            modifier = Modifier.weight(1f),
        )
        VaultOutlineButton(
            text = "Hapus",
            onClick = { vm.deleteStego(item) },
            modifier = Modifier.weight(1f),
        )
    }
}
