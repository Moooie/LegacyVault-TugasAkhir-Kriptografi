package id.upnyk.legacyvault.core.hardware

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import id.upnyk.legacyvault.core.crypto.DongleHmacVerifier

/**
 * Kriteria 1 — Lapis 2 (Storage Key) via Storage Access Framework.
 * Membaca / menulis `.legacy_dongle.key` pada root partisi SDHC yang dipilih pengguna,
 * lalu menyerahkan isinya ke [DongleHmacVerifier].
 */
class DongleStorageManager(private val context: Context) {

    companion object {
        const val KEY_FILE_NAME = ".legacy_dongle.key"
        private const val PREFS = "legacy_dongle_prefs"
        private const val PREF_TREE_URI = "tree_uri"
        private const val PREF_FILE_URI = "file_uri"
    }

    data class ReadResult(
        val success: Boolean,
        val message: String,
        val uri: String = "",
        val content: String = "",
        val verification: DongleHmacVerifier.Verification? = null,
    )

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Identitas volume dari documentId: "primary", "home", atau UUID kartu/flashdisk (mis. 1A2B-3C4D). */
    fun volumeId(treeUri: Uri): String = runCatching {
        android.provider.DocumentsContract.getTreeDocumentId(treeUri).substringBefore(':')
    }.getOrDefault("")

    /**
     * Volume tujuan harus media eksternal (kartu SD / flashdisk), bukan penyimpanan internal.
     * "primary"/"home" = internal. Selain itu documentId hanya diterbitkan untuk media lepas.
     */
    fun isRemovableVolume(treeUri: Uri): Boolean {
        val id = volumeId(treeUri)
        if (id.isBlank()) return false
        if (id.equals("primary", true) || id.equals("home", true)) return false
        val sm = context.getSystemService(android.os.storage.StorageManager::class.java)
        val match = sm?.storageVolumes?.firstOrNull { it.uuid?.equals(id, true) == true }
        // ponytail: sebagian ROM menyembunyikan uuid; non-primary sudah cukup sebagai bukti media lepas.
        return match?.isRemovable ?: true
    }

    /** Label ramah untuk volume: "SDHC 1A2B-3C4D" bila sistem mengenalinya. */
    fun volumeLabel(treeUri: Uri): String {
        val id = volumeId(treeUri)
        val sm = context.getSystemService(android.os.storage.StorageManager::class.java)
        val description = sm?.storageVolumes
            ?.firstOrNull { it.uuid?.equals(id, true) == true }
            ?.getDescription(context)
        return if (description.isNullOrBlank()) id else "$description · $id"
    }

    fun savedTreeUri(): Uri? = prefs.getString(PREF_TREE_URI, null)?.let(Uri::parse)

    fun savedFileUri(): Uri? = prefs.getString(PREF_FILE_URI, null)?.let(Uri::parse)

    fun takePersistablePermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }
        prefs.edit().putString(PREF_TREE_URI, treeUri.toString()).apply()
    }

    fun forget() {
        prefs.edit().clear().apply()
    }

    /** Cari berkas dongle di dalam folder yang sudah diberi izin. */
    fun resolveDongleFile(treeUri: Uri): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        root.findFile(KEY_FILE_NAME)?.let { return it }
        root.listFiles().forEach { child ->
            if (child.isDirectory) {
                child.findFile(KEY_FILE_NAME)?.let { return it }
            }
        }
        return null
    }

    fun readAndVerify(treeUri: Uri): ReadResult {
        val file = resolveDongleFile(treeUri)
            ?: return ReadResult(false, "Berkas $KEY_FILE_NAME tidak ditemukan pada partisi terpilih.")
        val content = runCatching {
            context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return ReadResult(false, "Gagal membaca berkas dongle.", file.uri.toString())

        val verification = DongleHmacVerifier.verify(content)
        prefs.edit().putString(PREF_FILE_URI, file.uri.toString()).apply()
        return ReadResult(
            success = verification.valid,
            message = verification.reason,
            uri = file.uri.toString(),
            content = content.trim(),
            verification = verification,
        )
    }

    /** Generator Dongle Key: menulis ulang berkas lisensi di partisi terpilih. */
    fun writeDongleFile(treeUri: Uri, serial: String): ReadResult {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return ReadResult(false, "Folder tidak dapat dibuka.")
        val existing = root.findFile(KEY_FILE_NAME)
        val target = existing ?: root.createFile("application/octet-stream", KEY_FILE_NAME)
            ?: return ReadResult(false, "Gagal membuat berkas di folder tersebut.")

        val issuedAt = System.currentTimeMillis() / 1000
        val content = DongleHmacVerifier.buildTokenFile(serial, issuedAt)
        val written = runCatching {
            context.contentResolver.openOutputStream(target.uri, "wt")?.use {
                it.write(content.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)

        if (!written) return ReadResult(false, "Gagal menulis berkas dongle.", target.uri.toString())
        prefs.edit().putString(PREF_FILE_URI, target.uri.toString()).apply()
        return ReadResult(
            success = true,
            message = "Berkas dongle berhasil dibuat ulang.",
            uri = target.uri.toString(),
            content = content,
            verification = DongleHmacVerifier.verify(content),
        )
    }
}
