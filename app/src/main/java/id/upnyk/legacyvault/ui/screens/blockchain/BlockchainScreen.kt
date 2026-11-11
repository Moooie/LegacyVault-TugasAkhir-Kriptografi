package id.upnyk.legacyvault.ui.screens.blockchain

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.core.database.entity.BlockchainBlockEntity
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
import id.upnyk.legacyvault.ui.components.color
import id.upnyk.legacyvault.ui.theme.VaultAccent
import id.upnyk.legacyvault.ui.theme.VaultError
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Kriteria 7 — Buku Besar Akta Kepemilikan dengan Proof-of-Work dan Tamper Simulator. */
@Composable
fun BlockchainScreen(vm: VaultViewModel, onMenu: () -> Unit) {
    val state = vm.chain
    val blocks by vm.ledger.collectAsState()
    val verification = state.verification

    val pickDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::onChainDocumentPicked)
    }

    VaultScaffold(
        title = "Buku Besar Aset",
        subtitle = "Kriteria 7 · SHA-256 + Proof-of-Work",
        onMenu = onMenu,
        onLock = vm::lockVault,
        onInfo = vm::reopenTrace,
    ) {
        Eyebrow("Ledger lokal · difficulty 3 nol")
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                verification == null -> "Rantai belum diverifikasi"
                verification.valid -> "Rantai utuh"
                else -> "Rantai rusak"
            },
            style = MaterialTheme.typography.headlineLarge,
            color = if (verification?.valid == false) VaultError else VaultText,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            when {
                verification == null -> "${blocks.size} blok siap diverifikasi."
                verification.valid -> "Seluruh ${verification.blockStatus.size} blok cocok dengan hash-nya."
                else -> "Blok #${verification.brokenIndex} tidak lagi cocok dengan hash yang tercatat."
            },
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextMuted,
        )

        Spacer(Modifier.height(VaultSpacing.md))
        MetricGrid(
            listOf(
                Triple("Blok", "${blocks.size}", VaultText),
                Triple("Difficulty", "000", VaultText),
                Triple(
                    "Status",
                    when {
                        verification == null -> "idle"
                        verification.valid -> "valid"
                        else -> "rusak"
                    },
                    when {
                        verification?.valid == false -> VaultError
                        verification?.valid == true -> VaultAccent
                        else -> VaultTextMuted
                    },
                ),
            ),
        )

        Spacer(Modifier.height(VaultSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            VaultOutlineButton(
                text = "Verifikasi rantai",
                onClick = { vm.verifyChain() },
                modifier = Modifier.weight(1f),
            )
            VaultOutlineButton(
                text = "Reset ledger",
                onClick = vm::resetLedger,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.message.isNotBlank()) {
            Spacer(Modifier.height(VaultSpacing.sm))
            InfoLine(state.message, if (state.isError) PillTone.ERROR else PillTone.OK)
        }

        SectionDivider("Tambah aset", if (state.mining) "menambang…" else "siap")
        VaultTextField(
            value = state.assetTitle,
            onValueChange = vm::onChainAsset,
            label = "Nama aset",
            placeholder = "Sertifikat Tanah SHM 1234",
        )
        Spacer(Modifier.height(VaultSpacing.sm))
        VaultTextField(
            value = state.beneficiaryName,
            onValueChange = vm::onChainBeneficiary,
            label = "Ahli waris",
            placeholder = "Budi Santoso",
        )
        Spacer(Modifier.height(VaultSpacing.sm))
        VaultTextField(
            value = state.documentSha256,
            onValueChange = vm::onChainDocumentHash,
            label = "SHA-256 dokumen",
            singleLine = false,
            minLines = 2,
            placeholder = "tempel hash, atau hitung dari berkas",
        )
        Spacer(Modifier.height(VaultSpacing.sm))
        VaultOutlineButton(
            text = "Hitung hash dari berkas",
            onClick = { pickDocument.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(state.documentSource, style = MaterialTheme.typography.bodySmall, color = VaultTextFaint)

        Spacer(Modifier.height(VaultSpacing.md))
        VaultButton(
            text = if (state.mining) "Menambang… ${state.attempts} nonce" else "Tambang blok baru",
            onClick = vm::mineBlock,
            enabled = !state.mining,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionDivider("Rantai blok", "${blocks.size} blok")
        if (blocks.isEmpty()) {
            Text(
                "Ledger sedang diinisialisasi…",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        } else {
            blocks.forEachIndexed { index, block ->
                val status = verification?.blockStatus?.firstOrNull { it.index == block.blockIndex }
                BlockRow(block, status?.valid, status?.message, vm)
                if (index != blocks.lastIndex) Hairline(Modifier.padding(vertical = VaultSpacing.md))
            }
        }

        Spacer(Modifier.height(VaultSpacing.lg))
        Text(
            "Tamper Simulator mengubah assetTitle pada satu blok. verifyChain() menandai blok itu dan " +
                "seluruh rantai sesudahnya.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextFaint,
        )
    }
}

@Composable
private fun BlockRow(
    block: BlockchainBlockEntity,
    valid: Boolean?,
    statusMessage: String?,
    vm: VaultViewModel,
) {
    val tone = when (valid) {
        true -> PillTone.OK
        false -> PillTone.ERROR
        null -> PillTone.NEUTRAL
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "#${block.blockIndex} · ${block.assetTitle}",
                style = MaterialTheme.typography.titleMedium,
                color = VaultText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${block.beneficiaryName} · nonce ${block.nonce} · " +
                    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(block.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StatusPill(
            when (valid) { true -> "valid"; false -> "rusak"; null -> "cek" },
            tone,
        )
    }

    if (statusMessage != null) {
        Spacer(Modifier.height(4.dp))
        InfoLine(statusMessage, tone)
    }

    Disclosure("Hash blok") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("CURRENT HASH", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
            MonoBlock(block.currentHash, maxLines = 2, tint = tone.color())
            Text("PREVIOUS HASH", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
            MonoBlock(block.previousHash, maxLines = 2)
            Text("DOCUMENT SHA-256", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
            MonoBlock(block.documentSha256, maxLines = 2)
        }
    }

    if (block.blockIndex > 0) {
        Spacer(Modifier.height(VaultSpacing.xs))
        VaultOutlineButton(
            text = "Simulasi ubah data blok ini",
            onClick = { vm.tamperBlock(block.blockIndex) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
