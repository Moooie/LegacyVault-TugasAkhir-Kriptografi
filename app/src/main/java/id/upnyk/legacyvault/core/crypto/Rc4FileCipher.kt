package id.upnyk.legacyvault.core.crypto

import java.security.MessageDigest

/**
 * Kriteria 4 — Stream Cipher RC4 (ARCFOUR) ditulis manual byte-per-byte.
 * KSA mengisi dan mempermutasi S[0..255], PRGA membangkitkan keystream, cipher = keystream XOR plaintext.
 */
object Rc4FileCipher {

    data class Trace(
        val keyHex: String,
        val sBoxPreview: String,
        val keystreamPreview: String,
        val plainPreview: String,
        val cipherPreview: String,
    )

    fun ksa(key: ByteArray): IntArray {
        require(key.isNotEmpty()) { "Kunci RC4 tidak boleh kosong." }
        val s = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + (key[i % key.size].toInt() and 0xFF)) and 0xFF
            val tmp = s[i]; s[i] = s[j]; s[j] = tmp
        }
        return s
    }

    fun process(input: ByteArray, key: ByteArray): ByteArray {
        val s = ksa(key)
        val out = ByteArray(input.size)
        var i = 0
        var j = 0
        for (idx in input.indices) {
            i = (i + 1) and 0xFF
            j = (j + s[i]) and 0xFF
            val tmp = s[i]; s[i] = s[j]; s[j] = tmp
            val k = s[(s[i] + s[j]) and 0xFF]
            out[idx] = (input[idx].toInt() xor k).toByte()
        }
        return out
    }

    fun encrypt(plain: ByteArray, keyText: String): ByteArray =
        process(plain, keyText.toByteArray(Charsets.UTF_8))

    fun decrypt(cipher: ByteArray, keyText: String): ByteArray =
        process(cipher, keyText.toByteArray(Charsets.UTF_8))

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    fun trace(plain: ByteArray, keyText: String, preview: Int = 16): Trace {
        val key = keyText.toByteArray(Charsets.UTF_8)
        val s = ksa(key)

        // Replikasi PRGA untuk cuplikan keystream pertama.
        val stream = ByteArray(preview)
        var i = 0
        var j = 0
        val sc = s.copyOf()
        for (n in 0 until preview) {
            i = (i + 1) and 0xFF
            j = (j + sc[i]) and 0xFF
            val t = sc[i]; sc[i] = sc[j]; sc[j] = t
            stream[n] = sc[(sc[i] + sc[j]) and 0xFF].toByte()
        }
        val cipher = process(plain, key)
        return Trace(
            keyHex = key.toHex(),
            sBoxPreview = s.take(16).joinToString(" ") { it.toString().padStart(3) },
            keystreamPreview = stream.toHex(),
            plainPreview = plain.take(preview).toByteArray().toHex(),
            cipherPreview = cipher.take(preview).toByteArray().toHex(),
        )
    }
}
