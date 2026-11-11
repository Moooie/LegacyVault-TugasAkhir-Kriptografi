package id.upnyk.legacyvault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Kriteria 3: surat wasiat setelah Extended Vigenere + AES-256-CBC. */
@Entity(tableName = "will_messages")
data class WillMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val vigenerePreviewCipher: String,
    val superCipherPayload: String,
    val createdAt: Long,
)
