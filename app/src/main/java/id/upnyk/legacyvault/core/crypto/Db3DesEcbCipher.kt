package id.upnyk.legacyvault.core.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Kriteria 5 — Triple DES mode ECB untuk kolom sensitif tabel `critical_notes`.
 * Kunci 24 byte (192-bit) diturunkan dari passphrase lewat SHA-256.
 * ECB dipilih agar tiap field/record bisa didekripsi independen tanpa IV.
 */
object Db3DesEcbCipher {

    private const val TRANSFORMATION = "DESede/ECB/PKCS5Padding"
    private const val KEY_BYTES = 24

    fun deriveKey(passphrase: String): ByteArray =
        MessageDigest.getInstance("SHA-256")
            .digest(passphrase.toByteArray(Charsets.UTF_8))
            .copyOf(KEY_BYTES)

    fun encrypt(plain: String, key: ByteArray): String {
        require(key.size == KEY_BYTES) { "Kunci 3DES harus 24 byte, ditemukan ${key.size}." }
        if (plain.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "DESede"))
        return cipher.doFinal(plain.toByteArray(Charsets.UTF_8)).toHex()
    }

    fun decrypt(hex: String, key: ByteArray): String {
        if (hex.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "DESede"))
        return String(cipher.doFinal(hex.hexToBytes()), Charsets.UTF_8)
    }

    /** Cuplikan blok demi blok agar ECB terlihat di panel X-Ray. */
    fun blockPreview(cipherHex: String, blockBytes: Int = 8, maxBlocks: Int = 4): List<String> {
        val charsPerBlock = blockBytes * 2
        return cipherHex.chunked(charsPerBlock).take(maxBlocks)
    }
}
