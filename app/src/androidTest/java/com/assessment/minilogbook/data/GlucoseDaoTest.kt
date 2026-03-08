package com.assessment.minilogbook.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException

/**
 * Instrumented test for [GlucoseDao].
 *
 * This test uses an in-memory database to ensure that data does not persist
 * between tests and does not affect the actual device database.
 */
@RunWith(Parameterized::class)
class GlucoseDaoTest(
    private val inputValue: Double,
    private val expectedValue: Double
) {

    private lateinit var glucoseDao: GlucoseDao
    private lateinit var db: GlucoseDatabase

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Input: {0}, Expected: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(5.5, 5.5),
                arrayOf(10.0, 10.0),
                arrayOf(0.0, 0.0),
                arrayOf(18.0182, 18.0182)
            )
        }
    }

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            GlucoseDatabase::class.java
        ).build()
        glucoseDao = db.glucoseDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndReadParameterizedEntry() = runBlocking {
        val entry = GlucoseEntry(valueInMmol = inputValue, timestamp = System.currentTimeMillis())
        glucoseDao.insert(entry)
        val allEntries = glucoseDao.getAllEntries().first()

        // Find the entry we just inserted by its value
        val retrievedEntry = allEntries.find { it.valueInMmol == expectedValue }
        assertEquals("The value retrieved should match the parameterized expected value", expectedValue, retrievedEntry?.valueInMmol ?: -1.0, 0.0)
    }

    @Test
    @Throws(Exception::class)
    fun insertMultipleEntriesOrderedByTimestamp() = runBlocking {
        val entry1 = GlucoseEntry(valueInMmol = 5.0, timestamp = 1000L)
        val entry2 = GlucoseEntry(valueInMmol = 6.0, timestamp = 2000L)

        glucoseDao.insert(entry1)
        glucoseDao.insert(entry2)

        val allEntries = glucoseDao.getAllEntries().first()

        assertEquals(2, allEntries.size)
        // Check order (DESC by timestamp)
        assertEquals(6.0, allEntries[0].valueInMmol, 0.0)
        assertEquals(5.0, allEntries[1].valueInMmol, 0.0)
    }

    @Test
    @Throws(Exception::class)
    fun deleteAllEntries() = runBlocking {
        glucoseDao.insert(GlucoseEntry(valueInMmol = 5.5))
        glucoseDao.insert(GlucoseEntry(valueInMmol = 6.5))

        glucoseDao.deleteAll()

        val allEntries = glucoseDao.getAllEntries().first()
        assertEquals(0, allEntries.size)
    }

    @Test
    @Throws(Exception::class)
    fun deleteSingleEntry_removesOnlyThatEntry() = runBlocking {
        glucoseDao.insert(GlucoseEntry(valueInMmol = 3.0, timestamp = 1000L))
        glucoseDao.insert(GlucoseEntry(valueInMmol = 7.0, timestamp = 2000L))

        // Retrieve the inserted entries to get their auto-generated ids
        val inserted = glucoseDao.getAllEntries().first()
        val toDelete = inserted.first { it.valueInMmol == 3.0 }

        glucoseDao.delete(toDelete)

        val remaining = glucoseDao.getAllEntries().first()
        assertEquals(1, remaining.size)
        assertEquals(7.0, remaining[0].valueInMmol, 0.0)
    }

    @Test
    @Throws(Exception::class)
    fun deleteSingleEntry_listBecomesEmptyWhenOnlyOneEntry() = runBlocking {
        glucoseDao.insert(GlucoseEntry(valueInMmol = inputValue, timestamp = System.currentTimeMillis()))

        val inserted = glucoseDao.getAllEntries().first()
        assertEquals(1, inserted.size)

        glucoseDao.delete(inserted[0])

        val remaining = glucoseDao.getAllEntries().first()
        assertEquals(0, remaining.size)
    }

    @Test
    @Throws(Exception::class)
    fun deleteEntry_doesNotAffectOtherEntries() = runBlocking {
        val timestamps = listOf(1000L, 2000L, 3000L)
        timestamps.forEach { glucoseDao.insert(GlucoseEntry(valueInMmol = it.toDouble(), timestamp = it)) }

        val inserted = glucoseDao.getAllEntries().first()
        val toDelete = inserted.first { it.timestamp == 2000L }

        glucoseDao.delete(toDelete)

        val remaining = glucoseDao.getAllEntries().first()
        assertEquals(2, remaining.size)
        assertEquals(listOf(3000.0, 1000.0), remaining.map { it.valueInMmol })
    }

    @Test
    @Throws(Exception::class)
    fun deleteNonExistentEntry_doesNothing() = runBlocking {
        glucoseDao.insert(GlucoseEntry(valueInMmol = 5.0, timestamp = 1000L))

        // Entry with id=999 was never inserted
        val phantom = GlucoseEntry(id = 999, valueInMmol = 5.0, timestamp = 1000L)
        glucoseDao.delete(phantom)

        val remaining = glucoseDao.getAllEntries().first()
        assertEquals(1, remaining.size)
    }
}
