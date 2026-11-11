package id.upnyk.legacyvault

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import id.upnyk.legacyvault.core.hardware.UsbDongleReceiver
import id.upnyk.legacyvault.ui.VaultViewModel
import id.upnyk.legacyvault.ui.navigation.VaultNavGraph
import id.upnyk.legacyvault.ui.theme.LegacyVaultTheme
import id.upnyk.legacyvault.ui.theme.VaultBackground

/**
 * Entry point. FragmentActivity dipakai karena BiometricPrompt membutuhkannya.
 * Receiver USB OTG didaftarkan runtime untuk ACTION_USB_DEVICE_ATTACHED / DETACHED.
 */
class MainActivity : FragmentActivity() {

    private val usbReceiver = UsbDongleReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LegacyVaultTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = VaultBackground) {
                    val vm: VaultViewModel = viewModel()
                    VaultNavGraph(vm)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(
            this,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(usbReceiver) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        UsbDongleReceiver.refresh(this)
    }
}
