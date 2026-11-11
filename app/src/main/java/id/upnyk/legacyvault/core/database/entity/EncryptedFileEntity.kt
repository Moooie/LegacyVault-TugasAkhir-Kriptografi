package id.upnyk.legacyvault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Kriteria 4: metadata file yang badannya sudah dienkripsi RC4 ke .rc4vault. */
@Entity(tableName = "encrypted_files")
data class EncryptedFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalName: String,
    val mimeType: String,
    val originalSize: Long,
    val originalSha256Checksum: String,
    val encryptedFilePath: String,
    val encryptedAt: Long,
)
