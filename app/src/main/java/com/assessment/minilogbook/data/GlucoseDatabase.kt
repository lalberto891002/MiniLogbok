package com.assessment.minilogbook.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

enum class GlucoseUnit {
    MMOL_L, MG_DL
}

@Entity(tableName = "glucose_entries")
data class GlucoseEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val valueInMmol: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM glucose_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<GlucoseEntry>>

    @Insert
    suspend fun insert(entry: GlucoseEntry)

    @Delete
    suspend fun delete(entry: GlucoseEntry)

    @Query("DELETE FROM glucose_entries")
    suspend fun deleteAll()
}

@Database(entities = [GlucoseEntry::class], version = 1, exportSchema = false)
abstract class GlucoseDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao

    companion object {
        @Volatile
        private var INSTANCE: GlucoseDatabase? = null

        fun getDatabase(context: Context): GlucoseDatabase {
            return INSTANCE ?: synchronized(this) {
                // Load the SQLCipher native libraries before building the database
                System.loadLibrary("sqlcipher")

                val passphrase = PassphraseManager.getOrCreatePassphrase(context)
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GlucoseDatabase::class.java,
                    "glucose_database"
                )
                    .openHelperFactory(factory)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

