package com.assessment.minilogbook.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single blood glucose reading.
 *
 * @property id          Auto-generated primary key.
 * @property valueInMmol The glucose reading stored in mmol/L (canonical unit).
 *                       Conversion to mg/dL is performed at the UI layer only.
 * @property timestamp   Unix epoch milliseconds when the entry was recorded.
 *                       Defaults to the current system time at insertion.
 */
@Entity(tableName = "glucose_entries")
data class GlucoseEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val valueInMmol: Double,
    val timestamp: Long = System.currentTimeMillis()
)
