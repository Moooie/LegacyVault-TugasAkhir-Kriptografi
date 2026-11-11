package id.upnyk.legacyvault.core.crypto

import android.graphics.Bitmap
import java.security.MessageDigest
import java.util.Random
import kotlin.math.log10

/**
 * Kriteria 6 — Steganografi LSB 24-bit (kanal R,G,B) dengan urutan indeks diacak PRNG ber-seed.
 *
 * Alur: 32 bit pertama = panjang pesan (byte). Setelah itu 8 bit per byte UTF-8.
 * Setiap bit disisipkan ke bit ke-0 pada salah satu kanal warna.
 * Urutan penyisipan = permutasi aritmetika `pos(i) = (offset + i * step) mod totalChannel`
 * dengan `step` saling prima terhadap `totalChannel`, keduanya dibangkitkan `java.util.Random(seed)`.
 */
object LsbSteganographyEngine {

    data class EmbedResult(
        val bitmap: Bitmap,
        val payloadBits: Int,
        val messageBytes: Int,
        val mse: Double,
        val psnr: Double,
        val seed: Long,
        val offset: Long,
        val step: Long,
        val capacityBits: Int,
    )

    data class ExtractResult(
        val message: String,
        val payloadBits: Int,
        val seed: Long,
    )

    fun seedFromKey(stegoKey: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(stegoKey.toByteArray(Charsets.UTF_8))
        var seed = 0L
        for (i in 0 until 8) seed = (seed shl 8) or (digest[i].toLong() and 0xFF)
        return seed
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = a
        var y = b
        while (y != 0L) {
            val t = x % y; x = y; y = t
        }
        return x
    }

    private class Permutation(val total: Long, seed: Long) {
        val offset: Long
        val step: Long

        init {
            val rnd = Random(seed)
            offset = (rnd.nextLong() ushr 1) % total
            var candidate: Long
            do {
                candidate = (rnd.nextLong() ushr 1) % total + 1
            } while (gcd(candidate, total) != 1L)
            step = candidate
        }

        fun position(i: Long): Long = (offset + i * step) % total
    }

    fun embed(cover: Bitmap, message: String, stegoKey: String): EmbedResult {
        val w = cover.width
        val h = cover.height
        val total = w.toLong() * h.toLong() * 3L

        val payloadBytes = message.toByteArray(Charsets.UTF_8)
        val bits = ArrayList<Int>(32 + payloadBytes.size * 8)
        for (i in 31 downTo 0) bits.add((payloadBytes.size ushr i) and 1)
        for (b in payloadBytes) {
            for (i in 7 downTo 0) bits.add((b.toInt() ushr i) and 1)
        }
        require(bits.size <= total) {
            "Kapasitas tidak cukup. Butuh ${bits.size} bit, tersedia $total bit."
        }

        val seed = seedFromKey(stegoKey)
        val perm = Permutation(total, seed)

        val pixels = IntArray(w * h)
        cover.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in bits.indices) {
            val pos = perm.position(i.toLong())
            val pixelIndex = (pos / 3).toInt()
            val channel = (pos % 3).toInt()
            val shift = when (channel) { 0 -> 16; 1 -> 8; else -> 0 }
            val color = pixels[pixelIndex]
            val value = (color shr shift) and 0xFF
            val newValue = (value and 0xFE) or bits[i]
            pixels[pixelIndex] = (color and (0xFF shl shift).inv()) or (newValue shl shift)
        }

        val stego = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        stego.setPixels(pixels, 0, w, 0, 0, w, h)

        val mse = meanSquaredError(cover, stego)
        return EmbedResult(
            bitmap = stego,
            payloadBits = bits.size,
            messageBytes = payloadBytes.size,
            mse = mse,
            psnr = psnr(mse),
            seed = seed,
            offset = perm.offset,
            step = perm.step,
            capacityBits = total.toInt(),
        )
    }

    fun extract(stego: Bitmap, stegoKey: String): ExtractResult {
        val w = stego.width
        val h = stego.height
        val total = w.toLong() * h.toLong() * 3L
        val seed = seedFromKey(stegoKey)
        val perm = Permutation(total, seed)

        val pixels = IntArray(w * h)
        stego.getPixels(pixels, 0, w, 0, 0, w, h)

        fun bitAt(i: Long): Int {
            val pos = perm.position(i)
            val pixelIndex = (pos / 3).toInt()
            val channel = (pos % 3).toInt()
            val shift = when (channel) { 0 -> 16; 1 -> 8; else -> 0 }
            return (pixels[pixelIndex] shr shift) and 1
        }

        var length = 0L
        for (i in 0 until 32) length = (length shl 1) or bitAt(i.toLong()).toLong()
        require(length > 0 && 32 + length * 8 <= total) {
            "Header panjang tidak valid ($length byte). Stego-key salah atau gambar bukan hasil sisipan."
        }
        val out = ByteArray(length.toInt())
        for (b in out.indices) {
            var v = 0
            for (k in 0 until 8) v = (v shl 1) or bitAt(32L + b * 8L + k)
            out[b] = v.toByte()
        }
        return ExtractResult(
            message = String(out, Charsets.UTF_8),
            payloadBits = 32 + out.size * 8,
            seed = seed,
        )
    }

    fun meanSquaredError(a: Bitmap, b: Bitmap): Double {
        require(a.width == b.width && a.height == b.height) { "Dimensi gambar harus sama." }
        val w = a.width
        val h = a.height
        val pa = IntArray(w * h).also { a.getPixels(it, 0, w, 0, 0, w, h) }
        val pb = IntArray(w * h).also { b.getPixels(it, 0, w, 0, 0, w, h) }
        var sum = 0.0
        for (i in pa.indices) {
            val ca = pa[i]
            val cb = pb[i]
            for (shift in intArrayOf(16, 8, 0)) {
                val d = ((ca shr shift) and 0xFF) - ((cb shr shift) and 0xFF)
                sum += (d * d).toDouble()
            }
        }
        return sum / (w.toDouble() * h.toDouble() * 3.0)
    }

    fun psnr(mse: Double): Double =
        if (mse <= 0.0) 99.0 else 10.0 * log10((255.0 * 255.0) / mse)
}
