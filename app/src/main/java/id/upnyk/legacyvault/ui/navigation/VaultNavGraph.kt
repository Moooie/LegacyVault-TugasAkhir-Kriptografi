package id.upnyk.legacyvault.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.ui.GateStage
import id.upnyk.legacyvault.ui.VaultViewModel
import id.upnyk.legacyvault.ui.components.CryptoInspectorSheet
import id.upnyk.legacyvault.ui.components.Eyebrow
import id.upnyk.legacyvault.ui.components.Hairline
import id.upnyk.legacyvault.ui.components.NavRow
import id.upnyk.legacyvault.ui.components.VaultSpacing
import id.upnyk.legacyvault.ui.screens.auth.AuthScreen
import id.upnyk.legacyvault.ui.screens.blockchain.BlockchainScreen
import id.upnyk.legacyvault.ui.screens.dashboard.DashboardScreen
import id.upnyk.legacyvault.ui.screens.dashboard.VaultModule
import id.upnyk.legacyvault.ui.screens.dbnotes.DbNotesScreen
import id.upnyk.legacyvault.ui.screens.filevault.FileVaultScreen
import id.upnyk.legacyvault.ui.screens.gatekeeper.BiometricScreen
import id.upnyk.legacyvault.ui.screens.gatekeeper.GatekeeperScreen
import id.upnyk.legacyvault.ui.screens.stegano.SteganoScreen
import id.upnyk.legacyvault.ui.screens.willtext.WillTextScreen
import id.upnyk.legacyvault.ui.theme.VaultBackground
import id.upnyk.legacyvault.ui.theme.VaultSurface
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Navigasi berbasis state mesin:
 * DONGLE_LOCKED → BIOMETRIC_LOCKED → AUTH_LOGIN → VAULT_UNLOCKED.
 * Modul brankas dipilih dari sidebar (ModalNavigationDrawer), bukan route Navigation-Compose.
 */
@Composable
fun VaultNavGraph(vm: VaultViewModel) {
    val context = LocalContext.current
    val gate = vm.gate
    val toast = vm.toast
    val usb by vm.usbState.collectAsState()
    var module by remember { mutableStateOf<VaultModule?>(null) }

    val unlocked = gate.stage == GateStage.VAULT_UNLOCKED
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(usb) { vm.onUsbChanged(usb) }
    // Tombol back sistem: modul terbuka → kembali ke beranda, bukan keluar aplikasi.
    BackHandler(enabled = module != null) { module = null }
    LaunchedEffect(gate.stage) {
        if (!unlocked) {
            module = null
            if (drawer.isOpen) drawer.close()
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(3500)
            vm.clearToast()
        }
    }

    val stageContent: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(VaultBackground)) {
            AnimatedContent(
                targetState = gate.stage to module,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "vault-stage",
            ) { (stage, current) ->
                when (stage) {
                    GateStage.DONGLE_LOCKED -> {
                        val activity = context as? android.app.Activity
                        GatekeeperScreen(vm = vm, onExit = { activity?.finish() })
                    }
                    GateStage.BIOMETRIC_LOCKED -> BiometricScreen(vm)
                    GateStage.AUTH_LOGIN -> AuthScreen(vm)
                    GateStage.VAULT_UNLOCKED -> when (current) {
                        null -> DashboardScreen(
                            vm = vm,
                            onOpen = { module = it },
                            onMenu = { scope.launch { drawer.open() } },
                        )
                        VaultModule.WILL -> WillTextScreen(vm) { scope.launch { drawer.open() } }
                        VaultModule.FILES -> FileVaultScreen(vm) { scope.launch { drawer.open() } }
                        VaultModule.NOTES -> DbNotesScreen(vm) { scope.launch { drawer.open() } }
                        VaultModule.STEGO -> SteganoScreen(vm) { scope.launch { drawer.open() } }
                        VaultModule.CHAIN -> BlockchainScreen(vm) { scope.launch { drawer.open() } }
                    }
                }
            }

            CryptoInspectorSheet(trace = vm.trace, onDismiss = vm::dismissTrace)

            if (toast != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(VaultSpacing.md),
                ) {
                    Text(
                        toast,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VaultText,
                        modifier = Modifier
                            .background(VaultSurface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }

    // Drawer hanya dipasang setelah brankas terbuka: kalau kontennya kosong saat anchor
    // dihitung, ModalNavigationDrawer menganggap posisi tertutup = 0 dan sidebar langsung tampil.
    if (!unlocked) {
        stageContent()
        return
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
        drawerContent = {
            VaultDrawer(
                username = vm.auth.username.ifBlank { "Pengguna" },
                role = vm.auth.role,
                current = module,
                counts = counts,
                onSelect = {
                    module = it
                    scope.launch { drawer.close() }
                },
                onLock = {
                    scope.launch { drawer.close() }
                    vm.lockVault()
                },
            )
        },
        content = stageContent,
    )
}

@Composable
private fun VaultDrawer(
    username: String,
    role: String,
    current: VaultModule?,
    counts: Map<VaultModule, String>,
    onSelect: (VaultModule?) -> Unit,
    onLock: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = VaultBackground,
        drawerContentColor = VaultText,
        modifier = Modifier.width(292.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VaultSpacing.sm)
                .padding(top = VaultSpacing.xl, bottom = VaultSpacing.lg),
        ) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                Eyebrow("LegacyVault")
                Spacer(Modifier.height(6.dp))
                Text(username, style = MaterialTheme.typography.titleLarge, color = VaultText)
                Text(
                    "Peran $role · sesi AES-CBC",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultTextFaint,
                )
            }

            Spacer(Modifier.height(VaultSpacing.md))
            NavRow(
                title = "Beranda",
                subtitle = "Ringkasan gerbang & modul",
                onClick = { onSelect(null) },
                active = current == null,
            )
            Hairline(Modifier.padding(horizontal = 12.dp, vertical = VaultSpacing.sm))

            Text(
                "MODUL KRIPTOGRAFI",
                style = id.upnyk.legacyvault.ui.theme.EyebrowStyle,
                color = VaultTextFaint,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
            )
            VaultModule.entries.forEach { entry ->
                NavRow(
                    title = entry.title,
                    subtitle = entry.algorithm,
                    onClick = { onSelect(entry) },
                    trailing = counts[entry],
                    active = current == entry,
                )
            }

            Hairline(Modifier.padding(horizontal = 12.dp, vertical = VaultSpacing.sm))
            NavRow(
                title = "Kunci Brankas",
                subtitle = "Kembali ke STATE_1 (dongle)",
                onClick = onLock,
            )

            Spacer(Modifier.height(VaultSpacing.md))
            Row(Modifier.padding(horizontal = 12.dp)) {
                Text("STATE_4", style = MaterialTheme.typography.labelMedium, color = VaultTextFaint)
                Spacer(Modifier.weight(1f))
                Text(
                    "UPNYK · Kriptografi",
                    style = MaterialTheme.typography.labelMedium,
                    color = VaultTextFaint,
                )
            }
        }
    }
}
