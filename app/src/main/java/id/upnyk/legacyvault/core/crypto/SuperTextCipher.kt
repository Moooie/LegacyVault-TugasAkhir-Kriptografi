package id.upnyk.legacyvault.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Kriteria 3 — Super Enkripsi surat wasiat.
 * Tahap 1: Extended Vigenere atas 95 karakter ASCII tercetak (32..126).
 * Tahap 2: AES-256-CBC, kunci turunan SHA-256(passphrase), IV 16 byte acak.
 * Format simpan: HEX(IV) + ":" + BASE64(ciphertext).
 */
object SuperTextCipher {

    private const val ASCII_START = 32
    private const val ASCII_RANGE = 95
    private val random = SecureRandom()

    data class SuperTrace(
        val plain: String,
        val vigenereKey: String,
        val stage1: String,
        val aesKeyHex: String,
        val ivHex: String,
        val payload: String,
        val restored: String,
    )

    data class CharStep(
        val index: Int,
        val plainChar: Char,
        val keyChar: Char,
        val plainCode: Int,
        val keyCode: Int,
        val cipherCode: Int,
        val cipherChar: Char,
    )

    // ---------- Tahap 1: Extended Vigenere ----------

    fun extendedVigenere(text: String, key: String, encrypt: Boolean): String {
        require(key.isNotEmpty()) { "Kunci Vigenere tidak boleh kosong." }
        val out = StringBuilder(text.length)
        var keyPos = 0
        for (ch in text) {
            val p = ch.code
            if (p < ASCII_START || p > 126) {
                out.append(ch) // karakter di luar rentang dibiarkan apa adanya
                continue
            }
            val k = key[keyPos % key.length].code
            val kNorm = (k - ASCII_START).mod(ASCII_RANGE)
            val shifted = if (encrypt) (p - ASCII_START + kNorm) else (p - ASCII_START - kNorm)
            out.append((shifted.mod(ASCII_RANGE) + ASCII_START).toChar())
            keyPos++
        }
        return out.toString()
    }

    fun vigenereSteps(text: String, key: String, limit: Int = 10): List<CharStep> {
        if (key.isEmpty()) return emptyList()
        val steps = mutableListOf<CharStep>()
        var keyPos = 0
        for ((i, ch) in text.withIndex()) {
            val p = ch.code
            if (p < ASCII_START || p > 126) continue
            val keyChar = key[keyPos % key.length]
            val kNorm = (keyChar.code - ASCII_START).mod(ASCII_RANGE)
            val c = (p - ASCII_START + kNorm).mod(ASCII_RANGE) + ASCII_START
            steps += CharStep(i, ch, keyChar, p, keyChar.code, c, c.toChar())
            keyPos++
            if (steps.size >= limit) break
        }
        return steps
    }

    // ---------- Tahap 2: AES-256-CBC ----------

    fun deriveAes256Key(passphrase: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(passphrase.toByteArray(Charsets.UTF_8))

    fun aesEncrypt(plain: String, passphrase: String): Triple<String, String, String> {
        val key = deriveAes256Key(passphrase)
        val iv = ByteArray(16).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val payload = iv.toHex() + ":" + Base64.getEncoder().encodeToString(ct)
        return Triple(payload, key.toHex(), iv.toHex())
    }

    fun aesDecrypt(payload: String, passphrase: String): String {
        val sep = payload.indexOf(':')
        require(sep > 0) { "Payload super cipher tidak valid (tanpa ':')." }
        val iv = payload.substring(0, sep).hexToBytes()
        require(iv.size == 16) { "IV harus 16 byte, ditemukan ${iv.size}." }
        val ct = Base64.getDecoder().decode(payload.substring(sep + 1))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(deriveAes256Key(passphrase), "AES"), IvParameterSpec(iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    // ---------- Pipeline lengkap ----------

    fun encryptSuper(plain: String, vigenereKey: String, passphrase: String): SuperTrace {
        val stage1 = extendedVigenere(plain, vigenereKey, encrypt = true)
        val (payload, keyHex, ivHex) = aesEncrypt(stage1, passphrase)
        return SuperTrace(plain, vigenereKey, stage1, keyHex, ivHex, payload, plain)
    }

    /** @return Pair(isi tahap 1 setelah dekripsi AES, plaintext akhir) */
    fun decryptSuper(payload: String, vigenereKey: String, passphrase: String): Pair<String, String> {
        val stage1 = aesDecrypt(payload, passphrase)
        return stage1 to extendedVigenere(stage1, vigenereKey, encrypt = false)
    }
}
