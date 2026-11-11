package id.upnyk.legacyvault.core.hardware

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import id.upnyk.legacyvault.core.crypto.toHex
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.concurrent.Executor

/**
 * Kriteria 1 — Mekanisme biometrik.
 * Kunci AES dibuat di Android Keystore dengan `setUserAuthenticationRequired(true)`,
 * lalu Cipher tersebut diikat ke BiometricPrompt lewat CryptoObject.
 */
class BiometricGateHelper(private val activity: FragmentActivity) {

    data class Availability(val canAuthenticate: Boolean, val status: Int, val label: String)

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun availability(): Availability {
        val manager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val status = manager.canAuthenticate(authenticators)
        val label = when (status) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Sensor biometrik kuat siap dipakai."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Perangkat tidak memiliki sensor biometrik."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Sensor biometrik sedang tidak tersedia."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Belum ada sidik jari terdaftar di perangkat."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Pembaruan keamanan perangkat diperlukan."
            else -> "Biometrik tidak dapat dipakai (status=$status)."
        }
        return Availability(status == BiometricManager.BIOMETRIC_SUCCESS, status, label)
    }

    private fun getOrCreateKey(): SecretKey {
        keyStore.getKey(KEY_NAME, null)?.let { return it as SecretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    /** Menjalankan BiometricPrompt yang terikat CryptoObject. */
    fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: (cipherHex: String) -> Unit,
        onFailure: (message: String) -> Unit,
    ) {
        val cipher = try {
            Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            onFailure("Kunci Keystore tidak valid lagi karena pendaftaran biometrik berubah. Hapus data aplikasi untuk membuat ulang kunci.")
            return
        } catch (e: Exception) {
            onFailure("Gagal menyiapkan Cipher Keystore: ${e.message}")
            return
        }

        val executor: Executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val used = result.cryptoObject?.cipher
                    val hex = if (used != null && used.iv != null) {
                        val iv = used.iv
                        val payload = used.doFinal(BIOMETRIC_PLAINTEXT.toByteArray(Charsets.UTF_8))
                        "IV=${iv.toHex()} | CT=${payload.toHex()}"
                    } else {
                        "Cipher tidak terbawa pada hasil autentikasi."
                    }
                    onSuccess(hex)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure("Biometrik gagal ($errorCode): $errString")
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Cipher AES/GCM di Android Keystore hanya terbuka setelah sidik jari terverifikasi.")
            .setNegativeButtonText("Batal")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .build()

        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    fun resetKey() {
        runCatching { keyStore.deleteEntry(KEY_NAME) }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_NAME = "legacyvault_biometric_key"
        const val BIOMETRIC_PLAINTEXT = "LEGACYVAULT-BIOMETRIC-PROOF"
    }
}
