package id.upnyk.legacyvault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Kriteria 6: hasil steganografi LSB, selalu PNG nirrugi. */
@Entity(tableName = "stego_images")
data class StegoImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val coverImagePath: String,
    val stegoImagePath: String,
    val payloadBitLength: Int,
    val psnrScore: Double,
    val createdAt: Long,
)
