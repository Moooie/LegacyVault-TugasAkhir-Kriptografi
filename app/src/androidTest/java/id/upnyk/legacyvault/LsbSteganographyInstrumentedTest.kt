package id.upnyk.legacyvault

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.upnyk.legacyvault.core.crypto.LsbSteganographyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifikasi Kriteria 6 pada perangkat/emulator: `gradlew connectedDebugAndroidTest`.
 * Bitmap wajib nyata, jadi modul ini tidak bisa diuji di JVM biasa.
 */
@RunWith(AndroidJUnit4::class)
class LsbSteganographyInstrumentedTest {

    private fun gradientBitmap(w: Int = 320, h: Int = 240): Bitmap {
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val r = (x * 255 / w)
                val g = (y * 255 / h)
                val b = ((x + y) * 255 / (w + h))
                pixels[y * w + x] = Color.argb(255, r, g, b)
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    @Test
    fun embedThenExtractRestoresMessage() {
        val cover = gradientBitmap()
        val message = "Sertifikat tanah di lemari buku lantai 2 — kunci di laci meja."

        val embedded = LsbSteganographyEngine.embed(cover, message, "petunjuk-harta-2026")
        assertEquals(message.toByteArray(Charsets.UTF_8).size * 8 + 32, embedded.payloadBits)
        assertTrue("PSNR harus di atas 40 dB, aktual=${embedded.psnr}", embedded.psnr > 40.0)

        val extracted = LsbSteganographyEngine.extract(embedded.bitmap, "petunjuk-harta-2026")
        assertEquals(message, extracted.message)
        assertEquals(embedded.seed, extracted.seed)
    }

    @Test
    fun wrongStegoKeyFailsOrReturnsDifferentText() {
        val cover = gradientBitmap()
        val embedded = LsbSteganographyEngine.embed(cover, "pesan rahasia keluarga", "kunci-benar")
        val wrong = runCatching {
            LsbSteganographyEngine.extract(embedded.bitmap, "kunci-salah")
        }
        val restored = wrong.getOrNull()?.message
        assertNotEquals("pesan rahasia keluarga", restored)
    }

    @Test
    fun seedDependsOnKey() {
        assertNotEquals(
            LsbSteganographyEngine.seedFromKey("kunci-a"),
            LsbSteganographyEngine.seedFromKey("kunci-b"),
        )
        assertEquals(
            LsbSteganographyEngine.seedFromKey("stabil"),
            LsbSteganographyEngine.seedFromKey("stabil"),
        )
    }
}
