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
import com.assessment.minilogbook.data.GlucoseUnit
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
            val value = currentInput.toDoubleOrNull() ?: 0.0
            val fromUnit = if (newUnit == GlucoseUnit.MG_DL) GlucoseUnit.MMOL_L else GlucoseUnit.MG_DL
            _glucoseService.convertValue(value, fromUnit, newUnit).toString()
        } else ""

        _unit.value = newUnit
        _inputValue.value = convertedInput
    }

    fun onInputValueChanged(newValue: String) {
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
