package id.upnyk.legacyvault.ui.screens.dbnotes

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.core.database.entity.CriticalNoteEntity
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
import id.upnyk.legacyvault.ui.theme.VaultAccent
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Kriteria 5 — Catatan finansial krusial dengan 3DES mode ECB di tabel Room. */
@Composable
fun DbNotesScreen(vm: VaultViewModel, onMenu: () -> Unit) {
    val state = vm.notes
    val items by vm.criticalNotes.collectAsState()

    VaultScaffold(
        title = "Catatan Penting",
        subtitle = "Kriteria 5 · 3DES/ECB/PKCS5Padding",
        onMenu = onMenu,
        onLock = vm::lockVault,
        onInfo = vm::reopenTrace,
    ) {
        Eyebrow("Enkripsi kolom basis data")
        Spacer(Modifier.height(6.dp))
        Text("Catatan finansial", style = MaterialTheme.typography.headlineLarge, color = VaultText)
        Spacer(Modifier.height(3.dp))
        Text(
            "Nomor rekening, PIN brankas, seed phrase, dan nilai taksiran harta tersimpan sebagai hex 3DES.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextMuted,
        )

        Spacer(Modifier.height(VaultSpacing.lg))
        VaultTextField(
            value = state.label,
            onValueChange = vm::onNoteLabel,
            label = "Label catatan",
            placeholder = "Brankas kamar utama",
        )
        Spacer(Modifier.height(VaultSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            VaultTextField(
                value = state.category,
                onValueChange = vm::onNoteCategory,
                label = "Kategori",
                placeholder = "Deposito",
                modifier = Modifier.weight(1f),
            )
            VaultTextField(
                value = state.accountOrPin,
                onValueChange = vm::onNoteAccount,
                label = "Rekening / PIN",
                placeholder = "889900",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(VaultSpacing.sm))
        VaultTextField(
            value = state.secretNote,
            onValueChange = vm::onNoteSecret,
            label = "Catatan rahasia / seed phrase",
            singleLine = false,
            minLines = 3,
            placeholder = "abandon ability able …",
        )
        Spacer(Modifier.height(VaultSpacing.sm))
        VaultTextField(
            value = state.passphrase,
            onValueChange = vm::onNotePassphrase,
            label = "Passphrase kunci 3DES (24 byte)",
        )

        Spacer(Modifier.height(VaultSpacing.lg))
        VaultButton(
            text = if (state.busy) "Mengenkripsi…" else "Enkripsi & simpan",
            onClick = vm::saveNote,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.message.isNotBlank()) {
            Spacer(Modifier.height(VaultSpacing.sm))
            InfoLine(state.message, if (state.isError) PillTone.ERROR else PillTone.OK)
        }

        SectionDivider("Raw database view", "${items.size} baris critical_notes")
        if (items.isEmpty()) {
            Text(
                "Tabel masih kosong. Kolom sensitif selalu tersimpan sebagai hex 3DES-ECB.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        } else {
            items.forEachIndexed { index, item ->
                NoteRow(item, vm)
                if (index != items.lastIndex) Hairline(Modifier.padding(vertical = VaultSpacing.md))
            }
        }

        Spacer(Modifier.height(VaultSpacing.lg))
        Text(
            "Mode ECB memungkinkan dekripsi satu baris tanpa menyentuh baris lain — konsekuensi " +
                "yang diminta tugas.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextFaint,
        )
    }
}

@Composable
private fun NoteRow(item: CriticalNoteEntity, vm: VaultViewModel) {
    val revealed = vm.notes.revealed[item.id]
    var key by remember(item.id) { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                item.label,
                style = MaterialTheme.typography.titleMedium,
                color = VaultText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(item.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        }
        StatusPill(
            if (revealed == null) "terkunci" else "terbuka",
            if (revealed == null) PillTone.NEUTRAL else PillTone.OK,
        )
    }

    if (revealed != null) {
        Spacer(Modifier.height(VaultSpacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            PlainField("Kategori", revealed.first)
            PlainField("Rekening / PIN", revealed.second)
            PlainField("Catatan rahasia", revealed.third)
        }
    }

    Disclosure("Ciphertext 3DES") {
        CipherField("Kategori", item.encryptedCategory3Des)
        Spacer(Modifier.height(4.dp))
        CipherField("Rekening / PIN", item.encryptedAccountOrPin3Des)
        Spacer(Modifier.height(4.dp))
        CipherField("Catatan rahasia", item.encryptedSecretNote3Des)
    }

    Spacer(Modifier.height(VaultSpacing.sm))
    if (revealed == null) {
        VaultTextField(
            value = key,
            onValueChange = { key = it },
            label = "Kunci 3DES untuk membuka baris ini",
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(VaultSpacing.sm))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        VaultOutlineButton(
            text = if (revealed == null) "Dekripsi" else "Sembunyikan",
            onClick = {
                if (revealed == null) {
                    vm.revealNote(item, key)
                    key = ""
                } else {
                    vm.hideNote(item.id)
                }
            },
            enabled = revealed != null || key.isNotBlank(),
            modifier = Modifier.weight(1f),
        )
        VaultOutlineButton(
            text = "Hapus",
            onClick = { vm.deleteNote(item.id) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlainField(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextFaint,
            modifier = Modifier.weight(1f),
        )
        Text(
            value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            color = VaultAccent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CipherField(label: String, cipherHex: String) {
    Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
    MonoBlock(cipherHex.ifBlank { "(kosong)" }, maxLines = 3)
}
