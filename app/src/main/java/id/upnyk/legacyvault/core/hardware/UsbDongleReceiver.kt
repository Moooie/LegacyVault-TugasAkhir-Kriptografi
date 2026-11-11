package id.upnyk.legacyvault.core.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Kriteria 1 — Lapis 1 (Hardware).
 * Memantau ACTION_USB_DEVICE_ATTACHED / DETACHED dan mencari perangkat USB host
 * yang mengekspos interface Mass Storage (USB class 0x08).
 *
 * Sengaja TIDAK mencocokkan vendorId/productId: reader yang dipakai penguji bisa
 * memakai chip apa pun (Lexar, SanDisk, Realtek, Genesys). Yang dibuktikan lapis 1
 * adalah "ada media penyimpanan USB tercolok", bukan merek tertentu.
 */
class UsbDongleReceiver : BroadcastReceiver() {

    data class UsbState(
        val attached: Boolean = false,
        val vendorId: Int = 0,
        val productId: Int = 0,
        val deviceName: String = "",
        /** True bila perangkat punya interface Mass Storage — syarat lapis 1. */
        val isMassStorage: Boolean = false,
        val lastEvent: String = "Belum ada event USB.",
    )

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                // Lepas satu perangkat: pindai ulang supaya perangkat lain tetap terhitung.
                refresh(context)
            }
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> refresh(context)
        }
    }

    private fun Intent.usbDevice(): UsbDevice? =
        @Suppress("DEPRECATION")
        getParcelableExtra(UsbManager.EXTRA_DEVICE)

    companion object {
        private val _state = MutableStateFlow(UsbState())
        val state: StateFlow<UsbState> = _state

        private fun publish(next: UsbState) {
            _state.value = next
        }

        private fun isMassStorage(device: UsbDevice): Boolean {
            if (device.deviceClass == UsbConstants.USB_CLASS_MASS_STORAGE) return true
            for (i in 0 until device.interfaceCount) {
                if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) return true
            }
            return false
        }

        private fun fromDevice(device: UsbDevice): UsbState = UsbState(
            attached = true,
            vendorId = device.vendorId,
            productId = device.productId,
            deviceName = device.productName?.takeIf { it.isNotBlank() } ?: device.deviceName,
            isMassStorage = isMassStorage(device),
            lastEvent = "USB terpasang: ${device.deviceName}",
        )

        /** Sinkronkan state dengan perangkat yang sudah tercolok sebelum aplikasi dibuka. */
        fun refresh(context: Context) {
            val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            val devices = manager?.deviceList?.values.orEmpty()
            val storage = devices.firstOrNull { isMassStorage(it) }
            publish(
                when {
                    storage != null -> fromDevice(storage)
                    devices.isNotEmpty() -> UsbState(
                        lastEvent = "Ada USB terpasang, tetapi bukan media penyimpanan (mass storage).",
                    )
                    else -> UsbState(lastEvent = "Tidak ada perangkat USB OTG terpasang.")
                },
            )
        }

        fun hex(value: Int): String = "0x" + value.toString(16).uppercase().padStart(4, '0')

        fun probe(context: Context): List<String> {
            val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
            return manager.deviceList.values.map {
                val kind = if (isMassStorage(it)) "storage" else "non-storage"
                "${it.deviceName}  VID=${hex(it.vendorId)} PID=${hex(it.productId)}  $kind"
            }
        }
    }
}
