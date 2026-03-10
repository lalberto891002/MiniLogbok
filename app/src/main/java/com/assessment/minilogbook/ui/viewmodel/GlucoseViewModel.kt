package com.assessment.minilogbook.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.service.IGlucoseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class GlucoseState(
    val unit: GlucoseUnit = GlucoseUnit.MMOL_L,
    val average: Double = 0.0,
    val isLoading: Boolean = true
)

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

    val glucoseEntries: Flow<PagingData<GlucoseEntry>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        _glucoseDao.getAllEntries()
    }
        .flow
        .cachedIn(viewModelScope)

    val glucoseState: StateFlow<GlucoseState> = combine(
        _glucoseDao.getAverageValue(),
        _unit
    ) { avgMmol, unit ->
        val safeAvgMmol = avgMmol ?: 0.0
        GlucoseState(
            unit = unit,
            average = _glucoseService.fromMmol(safeAvgMmol, unit),
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

    fun convertValue(valueInMmol: Double, toUnit: GlucoseUnit): Double {
        return _glucoseService.fromMmol(valueInMmol, toUnit)
    }

    fun deleteEntry(entry: GlucoseEntry) {
        viewModelScope.launch {
            _glucoseDao.delete(entry)
        }
    }

    fun getGlucoseStatus(valueInMmol: Double): BloodGlucoseStatus {
        return _glucoseService.getGlucoseStatus(valueInMmol)
    }

    fun getGlucoseStatusByUnit(value: Double, unit: GlucoseUnit): BloodGlucoseStatus {
        return _glucoseService.getGlucoseStatusByUnit(value, unit)
    }

}
