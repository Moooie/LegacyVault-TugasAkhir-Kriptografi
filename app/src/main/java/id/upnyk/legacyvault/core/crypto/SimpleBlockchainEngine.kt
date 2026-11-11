package id.upnyk.legacyvault.core.crypto

import java.security.MessageDigest

/** Representasi satu blok pada buku besar aset. */
data class AssetBlock(
    val index: Int,
    val timestamp: Long,
    val assetTitle: String,
    val beneficiaryName: String,
    val documentSha256: String,
    val previousHash: String,
    val nonce: Long,
    val currentHash: String,
)

/**
 * Kriteria 7 — Blockchain sederhana dengan Proof-of-Work (3 nol di depan).
 * Hash blok = SHA256(index + timestamp + assetTitle + beneficiaryName + documentSha256 + previousHash + nonce).
 */
object SimpleBlockchainEngine {

    const val DIFFICULTY = 3
    const val DIFFICULTY_PREFIX = "000"
    val GENESIS_PREVIOUS_HASH: String = "0".repeat(64)

    data class MineResult(
        val block: AssetBlock,
        val attempts: Long,
        val elapsedMs: Long,
    )

    data class Verification(
        val valid: Boolean,
        val blockStatus: List<BlockStatus>,
        val brokenIndex: Int?,
    ) {
        val summary: String
            get() = if (valid) "Rantai utuh: ${blockStatus.size} blok terverifikasi."
            else "Rantai RUSAK pada blok #${brokenIndex ?: -1}."
    }

    data class BlockStatus(
        val index: Int,
        val hashMatches: Boolean,
        val linkMatches: Boolean,
        val meetsDifficulty: Boolean,
        val message: String,
    ) {
        val valid: Boolean get() = hashMatches && linkMatches && meetsDifficulty
    }

    fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)).toHex()

    fun canonical(index: Int, timestamp: Long, assetTitle: String, beneficiaryName: String,
                  documentSha256: String, previousHash: String, nonce: Long): String =
        "$index$timestamp$assetTitle$beneficiaryName$documentSha256$previousHash$nonce"

    fun computeHash(index: Int, timestamp: Long, assetTitle: String, beneficiaryName: String,
                    documentSha256: String, previousHash: String, nonce: Long): String =
        sha256Hex(canonical(index, timestamp, assetTitle, beneficiaryName, documentSha256, previousHash, nonce))

    fun mine(
        index: Int,
        timestamp: Long,
        assetTitle: String,
        beneficiaryName: String,
        documentSha256: String,
        previousHash: String = GENESIS_PREVIOUS_HASH,
        onAttempt: ((Long) -> Unit)? = null,
    ): MineResult {
        val started = System.currentTimeMillis()
        var nonce = 0L
        while (true) {
            val hash = computeHash(index, timestamp, assetTitle, beneficiaryName, documentSha256, previousHash, nonce)
            if (hash.startsWith(DIFFICULTY_PREFIX)) {
                return MineResult(
                    block = AssetBlock(index, timestamp, assetTitle, beneficiaryName, documentSha256, previousHash, nonce, hash),
                    attempts = nonce + 1,
                    elapsedMs = System.currentTimeMillis() - started,
                )
            }
            if (nonce % 2000L == 0L) onAttempt?.invoke(nonce)
            nonce++
        }
    }

    fun verifyChain(blocks: List<AssetBlock>): Verification {
        val statuses = mutableListOf<BlockStatus>()
        var brokenIndex: Int? = null
        for ((position, block) in blocks.withIndex()) {
            val expectedHash = computeHash(
                block.index, block.timestamp, block.assetTitle, block.beneficiaryName,
                block.documentSha256, block.previousHash, block.nonce,
            )
            val hashMatches = expectedHash == block.currentHash
            val linkMatches = if (position == 0) {
                block.previousHash == GENESIS_PREVIOUS_HASH
            } else {
                block.previousHash == blocks[position - 1].currentHash
            }
            val meetsDifficulty = block.currentHash.startsWith(DIFFICULTY_PREFIX)
            val message = when {
                !linkMatches -> "previousHash tidak menunjuk ke hash blok sebelumnya."
                !hashMatches -> "currentHash tidak cocok dengan isi blok (data diubah)."
                !meetsDifficulty -> "Hash tidak memenuhi difficulty $DIFFICULTY_PREFIX."
                else -> "Valid."
            }
            val status = BlockStatus(block.index, hashMatches, linkMatches, meetsDifficulty, message)
            statuses += status
            if (!status.valid && brokenIndex == null) brokenIndex = block.index
        }
        return Verification(brokenIndex == null, statuses, brokenIndex)
    }
}
