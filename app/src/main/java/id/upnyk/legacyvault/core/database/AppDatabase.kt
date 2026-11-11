package id.upnyk.legacyvault.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.upnyk.legacyvault.core.database.dao.BlockchainBlockDao
import id.upnyk.legacyvault.core.database.dao.CriticalNoteDao
import id.upnyk.legacyvault.core.database.dao.EncryptedFileDao
import id.upnyk.legacyvault.core.database.dao.StegoImageDao
import id.upnyk.legacyvault.core.database.dao.UserDao
import id.upnyk.legacyvault.core.database.dao.WillMessageDao
import id.upnyk.legacyvault.core.database.entity.BlockchainBlockEntity
import id.upnyk.legacyvault.core.database.entity.CriticalNoteEntity
import id.upnyk.legacyvault.core.database.entity.EncryptedFileEntity
import id.upnyk.legacyvault.core.database.entity.StegoImageEntity
import id.upnyk.legacyvault.core.database.entity.UserEntity
import id.upnyk.legacyvault.core.database.entity.WillMessageEntity

@Database(
    entities = [
        UserEntity::class,
        WillMessageEntity::class,
        CriticalNoteEntity::class,
        EncryptedFileEntity::class,
        StegoImageEntity::class,
        BlockchainBlockEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun willMessageDao(): WillMessageDao
    abstract fun criticalNoteDao(): CriticalNoteDao
    abstract fun encryptedFileDao(): EncryptedFileDao
    abstract fun stegoImageDao(): StegoImageDao
    abstract fun blockchainBlockDao(): BlockchainBlockDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "legacy_vault.db",
                ).fallbackToDestructiveMigration(true).build().also { instance = it }
            }
    }
}
