package id.upnyk.legacyvault.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.upnyk.legacyvault.core.database.entity.BlockchainBlockEntity
import id.upnyk.legacyvault.core.database.entity.CriticalNoteEntity
import id.upnyk.legacyvault.core.database.entity.EncryptedFileEntity
import id.upnyk.legacyvault.core.database.entity.StegoImageEntity
import id.upnyk.legacyvault.core.database.entity.UserEntity
import id.upnyk.legacyvault.core.database.entity.WillMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun observeAll(): Flow<List<UserEntity>>
}

@Dao
interface WillMessageDao {
    @Insert
    suspend fun insert(message: WillMessageEntity): Long

    @Query("DELETE FROM will_messages WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM will_messages ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WillMessageEntity>>
}

@Dao
interface CriticalNoteDao {
    @Insert
    suspend fun insert(note: CriticalNoteEntity): Long

    @Update
    suspend fun update(note: CriticalNoteEntity)

    @Query("DELETE FROM critical_notes WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM critical_notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CriticalNoteEntity>>
}

@Dao
interface EncryptedFileDao {
    @Insert
    suspend fun insert(file: EncryptedFileEntity): Long

    @Query("DELETE FROM encrypted_files WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM encrypted_files ORDER BY encryptedAt DESC")
    fun observeAll(): Flow<List<EncryptedFileEntity>>
}

@Dao
interface StegoImageDao {
    @Insert
    suspend fun insert(image: StegoImageEntity): Long

    @Query("DELETE FROM stego_images WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM stego_images ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StegoImageEntity>>
}

@Dao
interface BlockchainBlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: BlockchainBlockEntity)

    @Query("DELETE FROM blockchain_ledger WHERE blockIndex > 0")
    suspend fun deleteAllExceptGenesis()

    @Query("UPDATE blockchain_ledger SET assetTitle = :tampered WHERE blockIndex = :index")
    suspend fun tamperTitle(index: Int, tampered: String)

    @Query("SELECT * FROM blockchain_ledger ORDER BY blockIndex ASC")
    fun observeAll(): Flow<List<BlockchainBlockEntity>>

    @Query("SELECT * FROM blockchain_ledger ORDER BY blockIndex ASC")
    suspend fun getAll(): List<BlockchainBlockEntity>

    @Query("SELECT * FROM blockchain_ledger WHERE blockIndex = :index LIMIT 1")
    suspend fun get(index: Int): BlockchainBlockEntity?

    @Query("SELECT COUNT(*) FROM blockchain_ledger")
    suspend fun count(): Int
}
