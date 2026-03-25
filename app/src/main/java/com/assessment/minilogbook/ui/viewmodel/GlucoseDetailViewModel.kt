package com.assessment.minilogbook.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.domain.service.IGlucoseService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the glucose entry detail screen.
 *
 * Receives [GlucoseDao] and [IGlucoseService] as data sources.
 * The [entryId] is read from [SavedStateHandle] (populated automatically by Navigation Compose).
 */
class GlucoseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val glucoseDao: GlucoseDao,
    private val glucoseService: IGlucoseService
) : ViewModel() {

    private val entryId: Int = checkNotNull(savedStateHandle["entryId"])

    /** The observed [GlucoseEntry]; null while loading or if not found. */
    val entry: StateFlow<GlucoseEntry?> = glucoseDao
        .getEntryById(entryId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** Converts a raw mmol/L value to the given [toUnit]. */
    fun convertValue(valueInMmol: Double, toUnit: GlucoseUnit): Double =
        glucoseService.fromMmol(valueInMmol, toUnit)

    /** Returns the [BloodGlucoseStatus] for [valueInMmol]. */
    fun getStatus(valueInMmol: Double): BloodGlucoseStatus =
        glucoseService.getGlucoseStatus(valueInMmol)
}


