package id.upnyk.legacyvault.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Kriteria 2 — Login aplikasi.
 *  - Hash password satu arah: SHA256(password + saltHex)
 *  - Token sesi dienkripsi JCrypto AES/CBC/PKCS5Padding 128-bit sebelum masuk SharedPreferences.
 */
object AuthLoginCrypto {

    private const val SALT_BYTES = 16
    private const val IV_BYTES = 16
    private val random = SecureRandom()

    data class SessionTicket(
        val ivHex: String,
        val cipherBase64: String,
        val keyHex: String,
    ) {
        fun serialize(): String = "$ivHex:$cipherBase64"
    }

    data class CredentialStep(
        val saltHex: String,
        val combined: String,
        val hashHex: String,
    )

    fun sha256Hex(input: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(input).toHex()

    fun randomSaltHex(): String = ByteArray(SALT_BYTES).also(random::nextBytes).toHex()

    fun randomIv(): ByteArray = ByteArray(IV_BYTES).also(random::nextBytes)

    /** SHA256(password + salt_hex) sesuai kontrak tugas. */
    fun hashPassword(password: String, saltHex: String): String =
        sha256Hex((password + saltHex).toByteArray(Charsets.UTF_8))

    fun credentialTrace(password: String, saltHex: String): CredentialStep {
        val combined = password + saltHex
        return CredentialStep(saltHex, combined, sha256Hex(combined.toByteArray(Charsets.UTF_8)))
    }

    /** Kunci AES-128 = 16 byte pertama dari SHA-256(passphrase). */
    fun deriveSessionKey(passphrase: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(passphrase.toByteArray(Charsets.UTF_8)).copyOf(16)

    fun encryptSession(plainJson: String, passphrase: String): SessionTicket {
        val key = deriveSessionKey(passphrase)
        val iv = randomIv()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val ct = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        return SessionTicket(iv.toHex(), java.util.Base64.getEncoder().encodeToString(ct), key.toHex())
    }

    fun decryptSession(ticket: String, passphrase: String): String {
        val sep = ticket.indexOf(':')
        require(sep > 0) { "Payload sesi tidak valid (tanpa pemisah ':')." }
        val iv = ticket.substring(0, sep).hexToBytes()
        val ct = java.util.Base64.getDecoder().decode(ticket.substring(sep + 1))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(deriveSessionKey(passphrase), "AES"), IvParameterSpec(iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }
}
