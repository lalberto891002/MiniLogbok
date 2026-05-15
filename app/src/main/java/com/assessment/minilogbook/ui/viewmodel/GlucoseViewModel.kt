package com.assessment.minilogbook.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.domain.service.IGlucoseService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class GlucoseState(
    val unit: GlucoseUnit = GlucoseUnit.MMOL_L,
    val average: Double = 0.0,
    val status: BloodGlucoseStatus = BloodGlucoseStatus.IN_TARGET,
    val isLoading: Boolean = true
)

/**
 * UI model for a single glucose entry shown in the history list.
 * Decouples the UI layer from the Room entity and pre-computes all display values
 * so the composable does not need to call ViewModel functions.
 *
 * @property id             Primary key (used for navigation to detail and delete).
 * @property valueInMmol    Raw value in mmol/L (canonical storage unit).
 * @property convertedValue Value converted to [unit] for display (via [IGlucoseService.fromMmol]).
 * @property status         Blood glucose status classification (via [IGlucoseService.getGlucoseStatus]).
 * @property timestamp      Unix epoch milliseconds for date formatting in the list item.
 * @property unit           The unit that [convertedValue] is expressed in.
 */
@Immutable
data class GlucoseListEntryUi(
    val id: Int,
    val valueInMmol: Double,
    val convertedValue: Double,
    val status: BloodGlucoseStatus,
    val timestamp: Long,
    val unit: GlucoseUnit
)

@OptIn(ExperimentalCoroutinesApi::class)
class GlucoseViewModel(
    private val _glucoseDao: GlucoseDao,
    private val _glucoseService: IGlucoseService
) : ViewModel() {

    companion object {
        /**
         * Maximum number of characters allowed in the glucose input field.
         * Covers both mmol/L (e.g. "33.3333") and mg/dL (e.g. "999.999") within 7 chars.
         */
        const val MAX_INPUT_LENGTH = 7

        /**
         * Regex that accepts an optional sequence of digits, followed by an optional single dot
         * and another optional sequence of digits. Empty string is also allowed (field cleared).
         * This prevents multiple dots ("1.2.3"), letters, or other non-numeric characters.
         */
        private val VALID_DECIMAL_REGEX = Regex("""^\d*\.?\d*$""")
    }

    private val _unit = MutableStateFlow(GlucoseUnit.MMOL_L)
    private val _inputValue = MutableStateFlow("")
    private val _displayErrorMessage = MutableStateFlow(false)
    val inputValue: StateFlow<String> = _inputValue.asStateFlow()
    val displayErrorMessage: StateFlow<Boolean> = _displayErrorMessage.asStateFlow()

    /**
     * Paging 3 flow of [GlucoseListEntryUi] items.
     *
     * Uses [flatMapLatest] on [_unit] so that whenever the selected unit changes, a fresh
     * [Pager] is created and every loaded page is re-mapped with the updated [GlucoseListEntryUi.convertedValue].
     * [cachedIn] keeps already-loaded pages alive for the duration of the ViewModel scope.
     */
    val glucoseEntries: Flow<PagingData<GlucoseListEntryUi>> = _unit
        .flatMapLatest { unit ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    prefetchDistance = 20,
                    enablePlaceholders = false
                )
            ) {
                _glucoseDao.getAllEntries()
            }.flow.map { pagingData ->
                pagingData.map { entry ->
                    GlucoseListEntryUi(
                        id = entry.id,
                        valueInMmol = entry.valueInMmol,
                        convertedValue = _glucoseService.fromMmol(entry.valueInMmol, unit),
                        status = _glucoseService.getGlucoseStatus(entry.valueInMmol),
                        timestamp = entry.timestamp,
                        unit = unit
                    )
                }
            }
        }
        .cachedIn(viewModelScope)

    val glucoseState: StateFlow<GlucoseState> = combine(
        _glucoseDao.getAverageValue(),
        _unit
    ) { avgMmol, unit ->
        val safeAvgMmol = avgMmol ?: 0.0
        val average = _glucoseService.fromMmol(safeAvgMmol, unit)
        GlucoseState(
            unit = unit,
            average = average,
            status = _glucoseService.getGlucoseStatusByUnit(average, unit),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GlucoseState(isLoading = true)
    )

    fun onUnitChanged(newUnit: GlucoseUnit) {
        val currentInput = _inputValue.value
        val convertedInput = if (currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull()
            val fromUnit = if (newUnit == GlucoseUnit.MG_DL) GlucoseUnit.MMOL_L else GlucoseUnit.MG_DL
            value?.let { _glucoseService.convertValue(it, fromUnit, newUnit).toString() } ?: currentInput
        } else ""

        _unit.value = newUnit
        _inputValue.value = convertedInput
    }

    fun onInputValueChanged(newValue: String) {
        if (newValue.length > MAX_INPUT_LENGTH) return
        if (!VALID_DECIMAL_REGEX.matches(newValue)) return
        _inputValue.value = newValue
        _displayErrorMessage.value = false
    }

    fun saveEntry() {
        val rawValue = _inputValue.value.toDoubleOrNull()
        val valueInMmol = _glucoseService.toMmolIfValid(rawValue, _unit.value)

        if (valueInMmol != null) {
            viewModelScope.launch {
                _glucoseDao.insert(GlucoseEntry(valueInMmol = valueInMmol))
                _inputValue.value = ""
                _displayErrorMessage.value = false
            }
        } else {
            _displayErrorMessage.value = true
        }
    }

    fun deleteEntry(entry: GlucoseListEntryUi) {
        viewModelScope.launch {
            _glucoseDao.deleteById(entry.id)
        }
    }
}
