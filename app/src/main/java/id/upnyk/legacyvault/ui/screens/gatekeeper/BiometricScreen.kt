package id.upnyk.legacyvault.ui.screens.gatekeeper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import id.upnyk.legacyvault.core.hardware.BiometricGateHelper
import id.upnyk.legacyvault.ui.VaultViewModel
import id.upnyk.legacyvault.ui.components.Disclosure
import id.upnyk.legacyvault.ui.components.Eyebrow
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
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted

/**
 * STATE_2 (BIOMETRIC_LOCKED) — lapis 2 saja.
 * Hanya muncul setelah lapis 1 lolos; tidak ada jalan kembali ke brankas tanpa CryptoObject.
 */
@Composable
fun BiometricScreen(vm: VaultViewModel) {
    val context = LocalContext.current
    val gate = vm.gate
    val activity = context as? FragmentActivity
    val helper = remember(activity) { activity?.let { BiometricGateHelper(it) } }

    LaunchedEffect(Unit) {
        helper?.let { vm.onBiometricAvailability(it.availability().canAuthenticate, it.availability().label) }
    }

    VaultScaffold(
        title = "LegacyVault",
        subtitle = "Lapis 2 · biometrik",
        onInfo = vm::reopenTrace,
    ) {
        Eyebrow("Langkah 2 dari 3 · terkunci")
        Spacer(Modifier.height(6.dp))
        Text("Verifikasi sidik jari", style = MaterialTheme.typography.headlineLarge, color = VaultText)
        Spacer(Modifier.height(4.dp))
        Text(
            "Dongle sudah lolos. Sekarang buka kunci AES di Android Keystore dengan biometrik perangkat.",
            style = MaterialTheme.typography.bodyMedium,
            color = VaultTextMuted,
        )

        Spacer(Modifier.height(VaultSpacing.md))
        StepRail(steps = listOf("Dongle" to true, "Biometrik" to gate.biometricPassed, "Login" to false))

        SectionDivider("Sensor perangkat", "Android Keystore")
        StatusPill(
            if (gate.biometricPassed) "Lolos" else "Menunggu",
            if (gate.biometricPassed) PillTone.OK else PillTone.WARN,
        )
        Spacer(Modifier.height(6.dp))
        LabeledValue("CryptoObject", gate.biometricLabel, tint = VaultTextMuted)
        if (gate.biometricMessage.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            InfoLine(gate.biometricMessage, if (gate.biometricPassed) PillTone.OK else PillTone.ERROR)
        }
        if (gate.biometricProof.isNotBlank()) {
            Disclosure("Detail kripto biometrik") {
                MonoBlock(gate.biometricProof, maxLines = 3)
            }
        }

        Spacer(Modifier.height(VaultSpacing.md))
        VaultButton(
            text = "Verifikasi sidik jari",
            onClick = {
                if (helper == null) {
                    vm.onBiometricFailure("Activity bukan FragmentActivity; BiometricPrompt tidak dapat dijalankan.")
                } else {
                    helper.authenticate(
                        title = "Buka LegacyVault",
                        subtitle = "Autentikasi mengikat Cipher ke Keystore",
                        onSuccess = vm::onBiometricSuccess,
                        onFailure = vm::onBiometricFailure,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (gate.simulationEnabled && !gate.biometricPassed) {
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultOutlineButton(
                text = "Lewati biometrik (debug)",
                onClick = vm::skipBiometricForDebug,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(VaultSpacing.sm))
        VaultOutlineButton(
            text = "Kembali ke pemeriksaan dongle",
            onClick = vm::backToDongle,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(VaultSpacing.lg))
        Column {
            Text(
                "Prompt biometrik dijalankan dengan CryptoObject sehingga pembukaan kunci " +
                    "terikat pada sensor, bukan hanya pada layar.",
                style = MaterialTheme.typography.bodySmall,
                color = VaultTextFaint,
            )
        }
    }
}
