package id.upnyk.legacyvault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Kriteria 7: buku besar aset lokal dengan Proof-of-Work. */
@Entity(tableName = "blockchain_ledger")
data class BlockchainBlockEntity(
    @PrimaryKey val blockIndex: Int,
    val timestamp: Long,
    val assetTitle: String,
    val beneficiaryName: String,
    val documentSha256: String,
    val previousHash: String,
    val nonce: Long,
    val currentHash: String,
)
