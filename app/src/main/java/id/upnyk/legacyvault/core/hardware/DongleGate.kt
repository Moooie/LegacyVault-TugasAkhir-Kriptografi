package id.upnyk.legacyvault.core.hardware

/**
 * Aturan lapis 1 tanpa ketergantungan Android, supaya bisa diuji sebagai unit murni.
 *
 * Lapis 1 lolos hanya bila:
 * 1. ada perangkat USB dengan interface mass storage tercolok, dan
 * 2. berkas kunci berada di volume eksternal (bukan "primary"/"home").
 */
object DongleGate {

    private val INTERNAL_VOLUMES = setOf("primary", "home")

    fun blocker(usbAttached: Boolean, isMassStorage: Boolean, simulated: Boolean = false): String? = when {
        simulated -> null
        !usbAttached -> "Dongle belum tercolok. Colokkan card reader atau flashdisk lewat OTG."
        !isMassStorage -> "Perangkat USB terdeteksi, tetapi bukan media penyimpanan (mass storage)."
        else -> null
    }

    fun isExternalVolumeId(volumeId: String?): Boolean {
        val id = volumeId?.trim().orEmpty()
        return id.isNotEmpty() && INTERNAL_VOLUMES.none { it.equals(id, ignoreCase = true) }
    }

    /** Media dongle dianggap sah bila reader tercolok dan kunci dibaca dari volume eksternal. */
    fun isDongleReady(usbAttached: Boolean, isMassStorage: Boolean, volumeRemovable: Boolean): Boolean =
        usbAttached && isMassStorage && volumeRemovable
}
