package id.upnyk.legacyvault.ui.screens.filevault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.core.database.entity.EncryptedFileEntity
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
import id.upnyk.legacyvault.ui.theme.MonoStyle
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Kriteria 4 — Brankas Dokumen Warisan dengan Stream Cipher RC4. */
@Composable
fun FileVaultScreen(vm: VaultViewModel, onMenu: () -> Unit) {
    val state = vm.fileVault
    val items by vm.encryptedFiles.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::onFilePicked)
    }

    VaultScaffold(
        title = "Dokumen Warisan",
        subtitle = "Kriteria 4 · RC4 KSA + PRGA",
        onMenu = onMenu,
        onLock = vm::lockVault,
        onInfo = vm::reopenTrace,
    ) {
        if (state.decryptTarget == null) {
            Eyebrow("Stream cipher · byte-per-byte")
            Spacer(Modifier.height(6.dp))
            Text("Kunci dokumen", style = MaterialTheme.typography.headlineLarge, color = VaultText)
            Spacer(Modifier.height(3.dp))
            Text(
                "PDF sertifikat, akta notaris, gambar, atau audio. Hasil disimpan sebagai .rc4vault.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextMuted,
            )

            Spacer(Modifier.height(VaultSpacing.lg))
            VaultOutlineButton(
                text = if (state.pickedName.isBlank()) "Pilih dokumen" else state.pickedName,
                onClick = { pickFile.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.pickedName.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ukuran asli ${state.pickedSize} byte",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextFaint,
                )
            }

            Spacer(Modifier.height(VaultSpacing.md))
            VaultTextField(
                value = state.passphrase,
                onValueChange = vm::onFilePassphrase,
                label = "Kunci RC4",
            )

            Spacer(Modifier.height(VaultSpacing.lg))
            VaultButton(
                text = if (state.busy) "Mengenkripsi…" else "Enkripsi dokumen",
                onClick = vm::encryptPickedFile,
                enabled = !state.busy && state.pickedUri != null,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Eyebrow("Dekripsi berkas")
            Spacer(Modifier.height(6.dp))
            Text(
                state.decryptTarget!!.originalName,
                style = MaterialTheme.typography.headlineLarge,
                color = VaultText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            MonoBlock(state.decryptTarget!!.encryptedFilePath, maxLines = 2)

            Spacer(Modifier.height(VaultSpacing.lg))
            VaultTextField(
                value = state.decryptPassphrase,
                onValueChange = vm::onFileDecryptPassphrase,
                label = "Kunci RC4",
            )
            Spacer(Modifier.height(VaultSpacing.lg))
            VaultButton(
                text = "Dekripsi",
                onClick = vm::decryptFile,
                enabled = state.decryptPassphrase.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultOutlineButton(
                text = "Tutup",
                onClick = vm::cancelFileDecrypt,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.integrityNote.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.md))
                InfoLine(
                    state.integrityNote,
                    if (state.integrityNote.startsWith("SHA-256 cocok")) PillTone.OK else PillTone.ERROR,
                )
            }
            if (state.decryptedText.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.md))
                Text("PRATINJAU HASIL", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
                Spacer(Modifier.height(4.dp))
                if (state.decryptedMime.startsWith("image/") && state.decryptedPath.isNotBlank()) {
                    DecryptedImage(state.decryptedPath)
                    Spacer(Modifier.height(6.dp))
                }
                Text(state.decryptedText, style = MonoStyle, color = VaultText)
            }
            if (state.decryptedPath.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.md))
                VaultOutlineButton(
                    text = "Buka dokumen dengan penampil",
                    onClick = { openDecryptedFile(context, state.decryptedPath, state.decryptedMime) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.message.isNotBlank()) {
            Spacer(Modifier.height(VaultSpacing.sm))
            InfoLine(state.message, if (state.isError) PillTone.ERROR else PillTone.OK)
        }

        SectionDivider("Arsip terenkripsi", "${items.size} berkas")
        if (items.isEmpty()) {
            Text(
                "Belum ada dokumen. Berkas hasil enkripsi disimpan di folder internal 'vault'.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        } else {
            items.forEachIndexed { index, item ->
                FileRow(item, vm)
                if (index != items.lastIndex) Hairline(Modifier.padding(vertical = VaultSpacing.md))
            }
        }

        Spacer(Modifier.height(VaultSpacing.lg))
        Text(
            "RC4 adalah stream cipher: tanpa padding, sehingga ukuran ciphertext identik dengan berkas asli.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextFaint,
        )
    }
}

@Composable
private fun FileRow(item: EncryptedFileEntity, vm: VaultViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                item.originalName,
                style = MaterialTheme.typography.titleMedium,
                color = VaultText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.originalSize} byte · " +
                    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(item.encryptedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        }
        StatusPill("RC4", PillTone.INFO)
    }

    Disclosure("Detail kripto") {
        Text("SHA-256 BERKAS ASLI", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
        MonoBlock(item.originalSha256Checksum, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Text("BERKAS TERENKRIPSI", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
        MonoBlock(item.encryptedFilePath, maxLines = 2)
    }

    Spacer(Modifier.height(VaultSpacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
        VaultOutlineButton(
            text = "Dekripsi",
            onClick = { vm.startFileDecrypt(item) },
            modifier = Modifier.weight(1f),
        )
        VaultOutlineButton(
            text = "Hapus",
            onClick = { vm.deleteFile(item) },
            modifier = Modifier.weight(1f),
        )
    }
}

/** Buka berkas hasil dekripsi (di cache) memakai penampil dokumen eksternal. */
private fun openDecryptedFile(context: android.content.Context, path: String, mime: String) {
    val file = java.io.File(path)
    if (!file.exists()) return
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime.ifBlank { "application/octet-stream" })
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(
            android.content.Intent.createChooser(intent, "Buka ${file.name}"),
        )
    }
}

@Composable
private fun DecryptedImage(path: String) {
    val bitmap = androidx.compose.runtime.remember(path) {
        android.graphics.BitmapFactory.decodeFile(path)
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Pratinjau hasil dekripsi",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}
