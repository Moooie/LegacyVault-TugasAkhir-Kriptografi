package id.upnyk.legacyvault.ui.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.core.hardware.UsbDongleReceiver
import id.upnyk.legacyvault.ui.VaultViewModel
import id.upnyk.legacyvault.ui.components.Disclosure
import id.upnyk.legacyvault.ui.components.Eyebrow
import id.upnyk.legacyvault.ui.components.MetricGrid
import id.upnyk.legacyvault.ui.components.MonoBlock
import id.upnyk.legacyvault.ui.components.NavRow
import id.upnyk.legacyvault.ui.components.SectionDivider
import id.upnyk.legacyvault.ui.components.VaultSpacing
import id.upnyk.legacyvault.ui.components.VaultScaffold
import id.upnyk.legacyvault.ui.theme.VaultAccent
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import id.upnyk.legacyvault.ui.theme.VaultWarning

enum class VaultModule(val title: String, val algorithm: String) {
    WILL("Surat Wasiat", "Vigenère + AES-256-CBC"),
    FILES("Dokumen Warisan", "RC4 stream cipher"),
    NOTES("Catatan Penting", "3DES mode ECB"),
    STEGO("Galeri Memori", "LSB + PRNG seed"),
    CHAIN("Buku Besar Aset", "SHA-256 + Proof-of-Work"),
}

/** STATE_4 — Beranda brankas. */
@Composable
fun DashboardScreen(
    vm: VaultViewModel,
    onOpen: (VaultModule) -> Unit,
    onMenu: () -> Unit,
) {
    val wills by vm.willMessages.collectAsState()
    val files by vm.encryptedFiles.collectAsState()
    val notes by vm.criticalNotes.collectAsState()
    val images by vm.stegoImages.collectAsState()
    val blocks by vm.ledger.collectAsState()
    val counts = mapOf(
        VaultModule.WILL to "${wills.size}",
        VaultModule.FILES to "${files.size}",
        VaultModule.NOTES to "${notes.size}",
        VaultModule.STEGO to "${images.size}",
        VaultModule.CHAIN to "${blocks.size}",
    )

    VaultScaffold(
        title = "Brankas",
        subtitle = "${vm.auth.username.ifBlank { "Pengguna" }} · ${vm.auth.role}",
        onMenu = onMenu,
        onLock = vm::lockVault,
        onInfo = vm::reopenTrace,
    ) {
        Eyebrow("State 4 · brankas terbuka")
        Spacer(Modifier.height(6.dp))
        Text(
            "Semua gerbang lolos",
            style = MaterialTheme.typography.headlineLarge,
            color = VaultText,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "Dongle, biometrik, dan kredensial terverifikasi pada sesi ini.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextMuted,
        )

        Spacer(Modifier.height(VaultSpacing.md))
        MetricGrid(
            listOf(
                Triple(
                    "Dongle",
                    when {
                        vm.gate.dongleVerified && !vm.gate.simulationUsed -> "Valid"
                        vm.gate.simulationUsed -> "Simulasi"
                        else -> "Terkunci"
                    },
                    when {
                        vm.gate.dongleVerified && !vm.gate.simulationUsed -> VaultAccent
                        vm.gate.simulationUsed -> VaultWarning
                        else -> VaultText
                    },
                ),
                Triple("Biometrik", if (vm.gate.biometricPassed) "Terbuka" else "Terkunci", VaultAccent),
                Triple("Sesi", "AES-CBC", VaultText),
                Triple("Basis data", "${notes.size} baris", VaultText),
            ),
        )

        SectionDivider("Modul kriptografi", "5 modul")
        VaultModule.entries.forEach { entry ->
            NavRow(
                title = entry.title,
                subtitle = entry.algorithm,
                onClick = { onOpen(entry) },
                trailing = counts[entry],
            )
            Spacer(Modifier.height(2.dp))
        }

        SectionDivider("Perangkat", "Kriteria 1")
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Dongle USB", style = MaterialTheme.typography.bodyMedium, color = VaultText)
                Text(
                    when {
                        vm.gate.usb.isMassStorage -> vm.gate.usb.deviceName
                        vm.gate.usb.attached -> "USB terpasang, bukan media penyimpanan"
                        else -> "Belum ada perangkat OTG terpasang"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextFaint,
                )
            }
            Text(
                if (vm.gate.usb.isMassStorage) {
                    UsbDongleReceiver.hex(vm.gate.usb.vendorId) + " / " + UsbDongleReceiver.hex(vm.gate.usb.productId)
                } else {
                    "mass storage"
                },
                style = MaterialTheme.typography.labelMedium,
                color = VaultTextFaint,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Volume kunci: " + vm.gate.partitionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextMuted,
        )

        SectionDivider("Penyimpanan", "Internal")
        Disclosure("Detail path & basis data") {
            Text("Folder vault", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
            MonoBlock(vm.vaultDirectory(), maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text("Basis data", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
            MonoBlock(vm.databasePath(), maxLines = 2)
        }

        Spacer(Modifier.height(VaultSpacing.lg))
        Text(
            "LegacyVault · Tugas Akhir Kriptografi UPNYK",
            style = MaterialTheme.typography.labelMedium,
            color = VaultTextFaint,
        )
    }
}
