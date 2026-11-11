package id.upnyk.legacyvault

import id.upnyk.legacyvault.core.crypto.AuthLoginCrypto
import id.upnyk.legacyvault.core.crypto.Db3DesEcbCipher
import id.upnyk.legacyvault.core.crypto.DongleHmacVerifier
import id.upnyk.legacyvault.core.crypto.Rc4FileCipher
import id.upnyk.legacyvault.core.crypto.SimpleBlockchainEngine
import id.upnyk.legacyvault.core.crypto.SuperTextCipher
import id.upnyk.legacyvault.core.crypto.toHex
import id.upnyk.legacyvault.core.hardware.DongleGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Satu check yang bisa dijalankan: `gradlew testDebugUnitTest`.
 * Menjaga kebenaran algoritma yang ditulis manual (Vigenère, RC4, HMAC, PoW) dan
 * round-trip cipher JCrypto (AES-CBC, 3DES-ECB, SHA-256+salt).
 */
class CryptoSelfCheckTest {

    @Test
    fun sha256KnownVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            AuthLoginCrypto.sha256Hex("abc".toByteArray()),
        )
    }

    @Test
    fun extendedVigenereRoundTripAndSpaceKeyIsIdentity() {
        val plain = "Wasiat 2026: tanah & rumah (Jogja) -> 3 anak."
        assertEquals(plain, SuperTextCipher.extendedVigenere(plain, " ", encrypt = true))

        val cipher = SuperTextCipher.extendedVigenere(plain, "WARISAN", encrypt = true)
        assertTrue(cipher != plain)
        assertEquals(plain, SuperTextCipher.extendedVigenere(cipher, "WARISAN", encrypt = false))
    }

    @Test
    fun superCipherVigenerePlusAesRoundTrip() {
        val plain = "Surat wasiat keluarga: rumah diserahkan kepada anak pertama."
        val trace = SuperTextCipher.encryptSuper(plain, "WARISAN", "kunci-induk-2026")
        assertTrue(trace.payload.contains(":"))
        assertEquals(32, trace.ivHex.length)

        val (stage1, restored) = SuperTextCipher.decryptSuper(trace.payload, "WARISAN", "kunci-induk-2026")
        assertEquals(trace.stage1, stage1)
        assertEquals(plain, restored)
    }

    @Test
    fun rc4MatchesPublishedTestVector() {
        val cipher = Rc4FileCipher.encrypt("Plaintext".toByteArray(), "Key")
        assertEquals("bbf316e8d940af0ad3", cipher.toHex())

        val back = Rc4FileCipher.decrypt(cipher, "Key")
        assertEquals("Plaintext", String(back, Charsets.UTF_8))
    }

    @Test
    fun tripleDesEcbRoundTripIsDeterministicPerBlock() {
        val key = Db3DesEcbCipher.deriveKey("kunci-brankas-2026")
        assertEquals(24, key.size)

        val cipher = Db3DesEcbCipher.encrypt("889900", key)
        assertEquals(16, cipher.length) // 6 byte -> satu blok 8 byte -> 16 hex char
        assertEquals("889900", Db3DesEcbCipher.decrypt(cipher, key))
        assertEquals(cipher, Db3DesEcbCipher.encrypt("889900", key)) // ECB deterministik
    }

    @Test
    fun dongleHmacAcceptsItsOwnSignatureAndRejectsTampering() {
        val file = DongleHmacVerifier.buildTokenFile("000000000819", 1794204977L)
        val ok = DongleHmacVerifier.verify(file)
        assertTrue(ok.valid)
        assertEquals("000000000819", ok.serial)

        val tampered = file.replace("000000000819", "000000000999")
        assertFalse(DongleHmacVerifier.verify(tampered).valid)
    }

    @Test
    fun dongleGateRequiresUsbStorageAndExternalVolume() {
        // Tanpa dongle: selalu diblokir, apa pun kondisi lainnya.
        assertTrue(DongleGate.blocker(usbAttached = false, isMassStorage = true) != null)
        // USB tercolok tetapi bukan mass storage (mis. mouse) tetap diblokir.
        assertTrue(DongleGate.blocker(usbAttached = true, isMassStorage = false) != null)
        // Media dongle tercolok: tidak ada penghalang.
        assertNull(DongleGate.blocker(usbAttached = true, isMassStorage = true))
        // Mode simulasi debug melewati lapis 1.
        assertNull(DongleGate.blocker(usbAttached = false, isMassStorage = false, simulated = true))

        // Penyimpanan internal HP tidak pernah diterima sebagai lokasi kunci.
        assertFalse(DongleGate.isExternalVolumeId("primary"))
        assertFalse(DongleGate.isExternalVolumeId("HOME"))
        assertFalse(DongleGate.isExternalVolumeId(""))
        // UUID kartu SD / flashdisk diterima.
        assertTrue(DongleGate.isExternalVolumeId("1A2B-3C4D"))

        assertFalse(DongleGate.isDongleReady(usbAttached = false, isMassStorage = true, volumeRemovable = true))
        assertFalse(DongleGate.isDongleReady(usbAttached = true, isMassStorage = true, volumeRemovable = false))
        assertTrue(DongleGate.isDongleReady(usbAttached = true, isMassStorage = true, volumeRemovable = true))
    }

    @Test
    fun aesSessionTokenRoundTrip() {        val json = """{"username":"notaris","role":"NOTARIS"}"""
        val ticket = AuthLoginCrypto.encryptSession(json, "rahasia-2026")
        assertTrue(ticket.serialize().contains(":"))
        assertEquals(json, AuthLoginCrypto.decryptSession(ticket.serialize(), "rahasia-2026"))
    }

    @Test
    fun proofOfWorkChainDetectsTampering() {
        val genesis = SimpleBlockchainEngine.mine(
            index = 0,
            timestamp = 1_735_689_600_000L,
            assetTitle = "GENESIS",
            beneficiaryName = "Sistem",
            documentSha256 = SimpleBlockchainEngine.sha256Hex("genesis"),
        ).block
        val second = SimpleBlockchainEngine.mine(
            index = 1,
            timestamp = 1_735_689_700_000L,
            assetTitle = "Sertifikat Tanah",
            beneficiaryName = "Budi",
            documentSha256 = SimpleBlockchainEngine.sha256Hex("dokumen"),
            previousHash = genesis.currentHash,
        ).block

        assertTrue(genesis.currentHash.startsWith("000"))
        assertTrue(second.currentHash.startsWith("000"))

        val chain = listOf(genesis, second)
        assertTrue(SimpleBlockchainEngine.verifyChain(chain).valid)

        val tampered = chain.toMutableList()
        tampered[1] = second.copy(assetTitle = "Sertifikat Tanah (DIUBAH)")
        val verdict = SimpleBlockchainEngine.verifyChain(tampered)
        assertFalse(verdict.valid)
        assertEquals(1, verdict.brokenIndex)
    }
}
