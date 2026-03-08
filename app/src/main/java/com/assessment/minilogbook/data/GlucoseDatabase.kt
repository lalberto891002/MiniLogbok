package com.assessment.minilogbook.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

    @Query("DELETE FROM glucose_entries")
    suspend fun deleteAll()
}

@Database(entities = [GlucoseEntry::class], version = 1, exportSchema = false)
abstract class GlucoseDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao

    companion object {
        @Volatile
        private var INSTANCE: GlucoseDatabase? = null

        fun getDatabase(context: android.content.Context): GlucoseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GlucoseDatabase::class.java,
                    "glucose_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

