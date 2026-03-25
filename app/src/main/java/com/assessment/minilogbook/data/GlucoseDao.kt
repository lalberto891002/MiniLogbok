package com.assessment.minilogbook.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [GlucoseEntry].
 *
 * All write operations are suspend functions and must be called from a coroutine.
 * Read operations return observable types ([Flow], [PagingSource]) so the UI reacts
 * automatically to database changes.
 */
@Dao
interface GlucoseDao {

    /**
     * Returns a [PagingSource] of all entries ordered newest-first.
     * Consumed by Paging 3 inside [com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel].
     */
    @Query("SELECT * FROM glucose_entries ORDER BY timestamp DESC")
    fun getAllEntries(): PagingSource<Int, GlucoseEntry>

    /**
     * Emits the average [GlucoseEntry.valueInMmol] across all stored entries,
     * or null when the table is empty.
     */
    @Query("SELECT AVG(valueInMmol) FROM glucose_entries")
    fun getAverageValue(): Flow<Double?>

    /**
     * Inserts a new [GlucoseEntry] into the database.
     *
     * @param entry The entry to persist.
     */
    @Insert
    suspend fun insert(entry: GlucoseEntry)

    /**
     * Deletes a specific [GlucoseEntry] matched by its primary key.
     *
     * @param entry The entry to remove.
     */
    @Delete
    suspend fun delete(entry: GlucoseEntry)

    /**
     * Removes all entries from the table.
     */
    @Query("DELETE FROM glucose_entries")
    suspend fun deleteAll()

    /**
     * Observes a single [GlucoseEntry] by [id]. Emits null if not found.
     */
    @Query("SELECT * FROM glucose_entries WHERE id = :id LIMIT 1")
    fun getEntryById(id: Int): Flow<GlucoseEntry?>
}
