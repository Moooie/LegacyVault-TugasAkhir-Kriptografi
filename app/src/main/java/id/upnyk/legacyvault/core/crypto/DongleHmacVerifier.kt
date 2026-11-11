package id.upnyk.legacyvault.core.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Kriteria 1 — Lapis 2 (Storage Key).
 * Verifikasi tanda tangan HMAC-SHA256 pada file `.legacy_dongle.key` di root partisi SDHC.
 *
 * Format berkas: `LEGACYVAULT-DONGLE-v1|serial|issuedEpoch|hmacHex`
 * HMAC dihitung atas string `LEGACYVAULT-DONGLE-v1|serial|issuedEpoch`.
 */
object DongleHmacVerifier {

    const val TOKEN_PREFIX = "LEGACYVAULT-DONGLE-v1"

    /** Secret demo yang ditanam di aplikasi. Ganti untuk produksi. */
    private const val SHARED_SECRET = "LegacyVault-Dongle-HMAC-2026"

    data class Verification(
        val valid: Boolean,
        val reason: String,
        val serial: String? = null,
        val issuedAt: Long? = null,
        val payload: String = "",
        val expectedHmac: String = "",
        val fileHmac: String = "",
    )

    fun hmacSha256(data: String, secret: String = SHARED_SECRET): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).toHex()
    }

    /** Membuat isi berkas dongle lengkap (dipakai fitur Generator Dongle Key). */
    fun buildTokenFile(serial: String, issuedAt: Long, secret: String = SHARED_SECRET): String {
        val payload = "$TOKEN_PREFIX|$serial|$issuedAt"
        return "$payload|${hmacSha256(payload, secret)}"
    }

    fun verify(rawContent: String, secret: String = SHARED_SECRET): Verification {
        val trimmed = rawContent.trim().replace("\r", "").replace("\n", "")
        if (trimmed.isEmpty()) {
            return Verification(false, "File dongle kosong.")
        }
        val parts = trimmed.split("|")
        if (parts.size != 4) {
            return Verification(false, "Format file salah. Harus 4 segmen dipisah '|', ditemukan ${parts.size}.", payload = trimmed)
        }
        if (parts[0] != TOKEN_PREFIX) {
            return Verification(false, "Prefiks token tidak dikenali: '${parts[0]}'.", payload = trimmed)
        }
        val serial = parts[1]
        val issuedAt = parts[2].toLongOrNull()
            ?: return Verification(false, "Epoch '${parts[2]}' bukan angka.", payload = trimmed)
        val payload = parts.subList(0, 3).joinToString("|")
        val expected = hmacSha256(payload, secret)
        val actual = parts[3].lowercase()

        if (!constantTimeEquals(expected, actual)) {
            return Verification(
                valid = false,
                reason = "HMAC tidak cocok. Berkas kemungkinan dimodifikasi atau secret berbeda.",
                serial = serial,
                issuedAt = issuedAt,
                payload = payload,
                expectedHmac = expected,
                fileHmac = actual,
            )
        }
        return Verification(
            valid = true,
            reason = "Tanda tangan HMAC-SHA256 valid.",
            serial = serial,
            issuedAt = issuedAt,
            payload = payload,
            expectedHmac = expected,
            fileHmac = actual,
        )
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}
