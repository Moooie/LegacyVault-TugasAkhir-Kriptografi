package id.upnyk.legacyvault.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Kriteria 1 & 2: identitas pengguna, hash SHA-256 + salt, dan ikatan dongle USB. */
@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val role: String,
    val passwordHashSha256: String,
    val saltHex: String,
    val boundUsbVid: Int,
    val boundUsbPid: Int,
    val dongleSignatureHmac: String,
)
