package id.upnyk.legacyvault.ui.screens.gatekeeper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.core.hardware.UsbDongleReceiver
import id.upnyk.legacyvault.ui.VaultViewModel
import id.upnyk.legacyvault.ui.components.Disclosure
import id.upnyk.legacyvault.ui.components.Eyebrow
import id.upnyk.legacyvault.ui.components.Hairline
import id.upnyk.legacyvault.ui.components.InfoLine
import id.upnyk.legacyvault.ui.components.LabeledValue
import id.upnyk.legacyvault.ui.components.MonoBlock
import id.upnyk.legacyvault.ui.components.PillTone
import id.upnyk.legacyvault.ui.components.SectionDivider
import id.upnyk.legacyvault.ui.components.StatusPill
import id.upnyk.legacyvault.ui.components.VaultButton
import id.upnyk.legacyvault.ui.components.VaultOutlineButton
import id.upnyk.legacyvault.ui.components.VaultScaffold
import id.upnyk.legacyvault.ui.components.VaultSpacing
import id.upnyk.legacyvault.ui.theme.VaultAccent
import id.upnyk.legacyvault.ui.theme.VaultBrand
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import id.upnyk.legacyvault.ui.theme.VaultWarning

/**
 * STATE_1 (DONGLE_LOCKED) — lapis 1 saja.
 * Aplikasi tidak bisa ke mana pun sebelum media dongle tercolok dan berkas kunci lolos HMAC.
 * Tombol "Batalkan" menutup aplikasi; biometrik baru muncul di layar berikutnya.
 */
@Composable
fun GatekeeperScreen(vm: VaultViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val gate = vm.gate
    var probe by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        vm.refreshUsb()
        probe = vm.usbProbe()
    }

    val pickPartition = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(vm::attachPartition) }

    val createDongleKey = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(vm::generateDongleKey) }

    VaultScaffold(
        title = "LegacyVault",
        subtitle = "Lapis 1 · dongle",
        onInfo = vm::reopenTrace,
    ) {
        Eyebrow("Langkah 1 dari 3 · terkunci")
        Spacer(Modifier.height(6.dp))
        Text("Masukkan dongle", style = MaterialTheme.typography.headlineLarge, color = VaultText)
        Spacer(Modifier.height(4.dp))
        Text(
            "Aplikasi hanya bisa dihidupkan bila media dongle tercolok dan berkas kuncinya sah. " +
                "Tanpa itu tidak ada jalan masuk.",
            style = MaterialTheme.typography.bodyMedium,
            color = VaultTextMuted,
        )

        Spacer(Modifier.height(VaultSpacing.md))
        StepRail(steps = listOf("Dongle" to gate.dongleOk, "Biometrik" to false, "Login" to false))

        SectionDivider("Pemeriksaan lapis 1", if (gate.dongleOk) "Lolos" else "Menunggu")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (gate.usb.isMassStorage) "Media USB terdeteksi" else "Media USB dongle",
                    style = MaterialTheme.typography.titleMedium,
                    color = VaultText,
                )
                Text(
                    if (gate.usb.isMassStorage) gate.usb.deviceName else "Belum ada card reader atau flashdisk OTG",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextFaint,
                )
            }
            StatusPill(
                if (gate.usb.isMassStorage) "Ada" else "Tidak ada",
                if (gate.usb.isMassStorage) PillTone.OK else PillTone.ERROR,
            )
        }

        Spacer(Modifier.height(VaultSpacing.sm))
        LabeledValue(
            "Lokasi berkas kunci",
            gate.partitionLabel,
            tint = if (gate.volumeRemovable) VaultText else VaultTextMuted,
        )
        Spacer(Modifier.height(10.dp))
        LabeledValue(
            "Tanda tangan HMAC-SHA256",
            gate.dongleMessage,
            tint = if (gate.dongleVerified) VaultAccent else VaultWarning,
        )

        Spacer(Modifier.height(VaultSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm)) {
            VaultOutlineButton(
                text = "Pilih lokasi",
                onClick = { pickPartition.launch(null) },
                enabled = gate.hardwareOk,
                modifier = Modifier.weight(1f),
            )
            VaultOutlineButton(
                text = "Verifikasi",
                onClick = vm::verifySavedPartition,
                enabled = gate.hardwareOk && gate.volumeRemovable,
                modifier = Modifier.weight(1f),
            )
        }
        if (!gate.hardwareOk) {
            Spacer(Modifier.height(6.dp))
            InfoLine("Colokkan dongle dulu — tombol lokasi terkunci sampai media terdeteksi.", PillTone.WARN)
        }

        Spacer(Modifier.height(VaultSpacing.md))
        VaultButton(
            text = "Lanjut ke biometrik",
            onClick = vm::advanceToBiometric,
            enabled = gate.dongleOk,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VaultSpacing.sm))
        VaultOutlineButton(
            text = "Batalkan",
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
        )

        Disclosure("Penyiapan dongle") {
            Text(
                "Tulis berkas lisensi ke dongle yang sedang tercolok. Berkas tidak bisa dibuat " +
                    "di penyimpanan internal HP.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextMuted,
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultOutlineButton(
                text = "Buat kunci di dongle",
                onClick = { createDongleKey.launch(null) },
                enabled = gate.hardwareOk,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultOutlineButton(
                text = "Pindai ulang USB",
                onClick = {
                    vm.refreshUsb()
                    probe = vm.usbProbe()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (probe.isNotEmpty()) {
                Spacer(Modifier.height(VaultSpacing.sm))
                MonoBlock(probe.joinToString("\n"), maxLines = 4)
            }
            Spacer(Modifier.height(VaultSpacing.sm))
            LabeledValue("Event USB terakhir", gate.usb.lastEvent, tint = VaultTextMuted)
            if (gate.dongleToken.isNotBlank()) {
                Spacer(Modifier.height(VaultSpacing.sm))
                Text("Isi berkas kunci", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
                MonoBlock(gate.dongleToken, maxLines = 3)
            }
        }

        if (gate.simulationEnabled) {
            Disclosure("Mode pengujian debug") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "ENABLE_DONGLE_SIMULATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = VaultWarning,
                        )
                        Text(
                            "Lewati lapis 1 saat dongle fisik belum tersedia. Hanya ada di build debug.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultTextMuted,
                        )
                    }
                    Switch(
                        checked = gate.simulationEnabled,
                        onCheckedChange = vm::toggleSimulation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = VaultBrand,
                        ),
                    )
                }
                Spacer(Modifier.height(VaultSpacing.sm))
                VaultOutlineButton(
                    text = "Gunakan dongle simulasi",
                    onClick = vm::useSimulatedDongle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(VaultSpacing.lg))
        Hairline()
        Spacer(Modifier.height(VaultSpacing.sm))
        Text(
            "Pemeriksaan: (1) medan USB mass storage harus tercolok, (2) berkas kunci harus berada " +
                "di volume kartu/flashdisk, bukan memori HP, (3) HMAC-SHA256 harus cocok.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextFaint,
        )
    }
}

/** Tiga langkah gerbang; lingkaran terisi menandai langkah yang sudah lolos. */
@Composable
fun StepRail(steps: List<Pair<String, Boolean>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, (label, done) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(if (done) VaultBrand else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (done) MaterialTheme.colorScheme.onPrimary else VaultTextFaint,
                    )
                }
                Spacer(Modifier.size(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (done) VaultText else VaultTextFaint,
                    maxLines = 1,
                )
            }
            if (index != steps.lastIndex) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(1.dp)
                        .background(if (done) VaultBrand.copy(alpha = 0.5f) else VaultTextFaint.copy(alpha = 0.2f)),
                )
            }
        }
    }
}
