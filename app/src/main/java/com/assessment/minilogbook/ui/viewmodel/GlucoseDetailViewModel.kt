package com.assessment.minilogbook.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.domain.service.IGlucoseService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI model for a single glucose entry. Decouples the UI layer from the Room entity.
 *
 * @property id            Primary key of the entry.
 * @property formattedDate Human-readable date/time string.
 * @property valueInMmol   Glucose value in mmol/L.
 * @property valueInMgdl   Glucose value converted to mg/dL.
 * @property status        Blood glucose status classification.
 */
@Immutable
data class GlucoseEntryUi(
    val id: Int,
    val formattedDate: String,
    val valueInMmol: Double,
    val valueInMgdl: Double,
    val status: BloodGlucoseStatus
)

/**
 * ViewModel for the glucose entry detail screen.
 *
 * Receives [GlucoseDao] and [IGlucoseService] as data sources.
 * The [entryId] is read from [SavedStateHandle] (populated automatically by Navigation Compose).
 */
class GlucoseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    glucoseDao: GlucoseDao,
    glucoseService: IGlucoseService
) : ViewModel() {

    private val entryId: Int = checkNotNull(savedStateHandle["entryId"])

    private val _glucoseDao = glucoseDao
    private val _glucoseService = glucoseService

    private val dateFormatter = DateTimeFormatter
        .ofPattern("EEEE, MMM dd yyyy  •  HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    /** The observed [GlucoseEntryUi]; null while loading or if not found. */
    val entry: StateFlow<GlucoseEntryUi?> = _glucoseDao
        .getEntryById(entryId)
        .map { raw ->
            raw?.let {
                GlucoseEntryUi(
                    id = it.id,
                    formattedDate = dateFormatter.format(Instant.ofEpochMilli(it.timestamp)),
                    valueInMmol = it.valueInMmol,
                    valueInMgdl = _glucoseService.fromMmol(it.valueInMmol, GlucoseUnit.MG_DL),
                    status = _glucoseService.getGlucoseStatus(it.valueInMmol)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}


