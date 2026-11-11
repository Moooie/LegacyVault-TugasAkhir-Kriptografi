package id.upnyk.legacyvault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Kriteria 5: kolom sensitif tersimpan sebagai hex 3DES-ECB. */
@Entity(tableName = "critical_notes")
data class CriticalNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val encryptedCategory3Des: String,
    val encryptedAccountOrPin3Des: String,
    val encryptedSecretNote3Des: String,
    val updatedAt: Long,
)
