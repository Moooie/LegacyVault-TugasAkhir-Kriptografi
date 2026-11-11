package id.upnyk.legacyvault.ui.screens.willtext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.core.database.entity.WillMessageEntity
import id.upnyk.legacyvault.ui.VaultViewModel
import id.upnyk.legacyvault.ui.components.Disclosure
import id.upnyk.legacyvault.ui.components.Eyebrow
import id.upnyk.legacyvault.ui.components.Hairline
import id.upnyk.legacyvault.ui.components.InfoLine
import id.upnyk.legacyvault.ui.components.MonoBlock
import id.upnyk.legacyvault.ui.components.PillTone
import id.upnyk.legacyvault.ui.components.SectionDivider
import id.upnyk.legacyvault.ui.components.StatusPill
import id.upnyk.legacyvault.ui.components.VaultButton
import id.upnyk.legacyvault.ui.components.VaultOutlineButton
import id.upnyk.legacyvault.ui.components.VaultScaffold
import id.upnyk.legacyvault.ui.components.VaultSpacing
import id.upnyk.legacyvault.ui.components.VaultTextField
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Kriteria 3 — Surat Wasiat dengan Super Enkripsi (Extended Vigenère → AES-256-CBC). */
@Composable
fun WillTextScreen(vm: VaultViewModel, onMenu: () -> Unit) {
    val state = vm.will
    val items by vm.willMessages.collectAsState()

    VaultScaffold(
        title = "Surat Wasiat",
        subtitle = "Kriteria 3 · Vigenère + AES-256-CBC",
        onMenu = onMenu,
        onLock = vm::lockVault,
        onInfo = vm::reopenTrace,
    ) {
        if (state.decryptTarget == null) {
            Eyebrow("Super enkripsi · 2 lapis")
            Spacer(Modifier.height(6.dp))
            Text("Tulis surat wasiat", style = MaterialTheme.typography.headlineLarge, color = VaultText)
            Spacer(Modifier.height(3.dp))
            Text(
                "Tahap 1 menyandikan 95 karakter ASCII, tahap 2 mengunci hasilnya dengan AES-256.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextMuted,
            )

            Spacer(Modifier.height(VaultSpacing.lg))
            VaultTextField(
                value = state.title,
                onValueChange = vm::onWillTitle,
                label = "Judul surat",
                placeholder = "Wasiat Keluarga Santoso",
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultTextField(
                value = state.plaintext,
                onValueChange = vm::onWillPlaintext,
                label = "Isi wasiat",
                placeholder = "Tuliskan pembagian warisan…",
                singleLine = false,
                minLines = 5,
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
                VaultTextField(
                    value = state.vigenereKey,
                    onValueChange = vm::onWillKey,
                    label = "Kunci Vigenère",
                    modifier = Modifier.weight(1f),
                )
                VaultTextField(
                    value = state.passphrase,
                    onValueChange = vm::onWillPassphrase,
                    label = "Passphrase AES",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(VaultSpacing.lg))
            VaultButton(
                text = if (state.busy) "Mengenkripsi…" else "Enkripsi & simpan",
                onClick = vm::saveWill,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.message.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.sm))
                InfoLine(state.message, if (state.isError) PillTone.ERROR else PillTone.OK)
            }
        } else {
            Eyebrow("Dekripsi surat")
            Spacer(Modifier.height(6.dp))
            Text(
                state.decryptTarget!!.title,
                style = MaterialTheme.typography.headlineLarge,
                color = VaultText,
            )
            Spacer(Modifier.height(3.dp))
            Disclosure("Ciphertext tersimpan") {
                MonoBlock(state.decryptTarget!!.superCipherPayload, maxLines = 3)
            }

            Spacer(Modifier.height(VaultSpacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
                VaultTextField(
                    value = state.decryptVigenereKey,
                    onValueChange = vm::onWillDecryptVigenereKey,
                    label = "Kunci Vigenère",
                    modifier = Modifier.weight(1f),
                )
                VaultTextField(
                    value = state.decryptPassphrase,
                    onValueChange = vm::onWillDecryptPassphrase,
                    label = "Passphrase AES",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(VaultSpacing.lg))
            VaultButton(
                text = "Dekripsi",
                onClick = vm::decryptWill,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultOutlineButton(
                text = "Tutup",
                onClick = vm::cancelWillDecrypt,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.decryptedText.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.lg))
                Text("PLAINTEXT TERPULIHKAN", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
                Spacer(Modifier.height(4.dp))
                Text(state.decryptedText, style = MaterialTheme.typography.bodyLarge, color = VaultText)
            }
            if (state.message.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.sm))
                InfoLine(state.message, if (state.isError) PillTone.ERROR else PillTone.OK)
            }
        }

        SectionDivider("Arsip surat", "${items.size} tersimpan")
        if (items.isEmpty()) {
            Text(
                "Belum ada surat. Surat yang disimpan muncul di sini sebagai ciphertext.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        } else {
            items.forEachIndexed { index, item ->
                WillRow(item, vm)
                if (index != items.lastIndex) Hairline(Modifier.padding(vertical = VaultSpacing.md))
            }
        }

        Spacer(Modifier.height(VaultSpacing.lg))
        Text(
            "Format simpan: HEX(IV) + \":\" + BASE64(ciphertext AES). Kolom vigenerePreviewCipher " +
                "menyimpan hasil tahap 1 untuk demonstrasi.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextFaint,
        )
    }
}

@Composable
private fun WillRow(item: WillMessageEntity, vm: VaultViewModel) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = VaultText, maxLines = 1)
            Text(
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(item.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        }
        StatusPill("super", PillTone.INFO)
    }

    Disclosure("Detail kripto") {
        Text("TAHAP 1 · VIGENÈRE", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
        MonoBlock(item.vigenerePreviewCipher, maxLines = 3)
        Spacer(Modifier.height(4.dp))
        Text("TAHAP 2 · AES-256", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
        MonoBlock(item.superCipherPayload, maxLines = 3)
    }

    Spacer(Modifier.height(VaultSpacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        VaultOutlineButton(
            text = "Buka",
            onClick = { vm.startWillDecrypt(item) },
            modifier = Modifier.weight(1f),
        )
        VaultOutlineButton(
            text = "Hapus",
            onClick = { vm.deleteWill(item.id) },
            modifier = Modifier.weight(1f),
        )
    }
}
