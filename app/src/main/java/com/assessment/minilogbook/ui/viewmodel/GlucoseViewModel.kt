package com.assessment.minilogbook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.usecase.GlucoseConverter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GlucoseState(
    val entries: List<GlucoseEntry> = emptyList(),
    val unit: GlucoseUnit = GlucoseUnit.MMOL_L,
    val inputValue: String = "",
    val errorMessage: String? = null
)

class GlucoseViewModel(
    private val glucoseDao: GlucoseDao,
    private val converter: GlucoseConverter = GlucoseConverter()
) : ViewModel() {

    private val _state = MutableStateFlow(GlucoseState())
    val state: StateFlow<GlucoseState> = _state.asStateFlow()

    init {
        glucoseDao.getAllEntries()
            .onEach { entries ->
                _state.update { it.copy(entries = entries) }
            }
            .launchIn(viewModelScope)
    }

    fun onUnitChanged(newUnit: GlucoseUnit) {
        val currentInput = _state.value.inputValue
        val convertedInput = if (currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull() ?: 0.0
            val fromUnit = if (newUnit == GlucoseUnit.MG_DL) GlucoseUnit.MMOL_L else GlucoseUnit.MG_DL
            converter.convertValue(value, fromUnit, newUnit).toString()
        } else ""

        _state.update { it.copy(unit = newUnit, inputValue = convertedInput) }
    }

    fun onInputValueChanged(newValue: String) {
        _state.update { it.copy(inputValue = newValue, errorMessage = null) }
    }

    fun saveEntry() {
        val rawValue = _state.value.inputValue.toDoubleOrNull()
        val valueInMmol = converter.toMmolIfValid(rawValue, _state.value.unit)

        if (valueInMmol != null) {
            viewModelScope.launch {
                glucoseDao.insert(GlucoseEntry(valueInMmol = valueInMmol))
                _state.update { it.copy(inputValue = "", errorMessage = null) }
            }
        } else {
            _state.update { it.copy(errorMessage = "Please enter a valid value >= 0") }
        }
    }

    fun getAverage(unit: GlucoseUnit): Double {
        if (_state.value.entries.isEmpty()) return 0.0
        val avgMmol = _state.value.entries.map { it.valueInMmol }.average()
        return converter.fromMmol(avgMmol, unit)
    }

    fun convertValue(valueInMmol: Double, toUnit: GlucoseUnit): Double {
        return converter.fromMmol(valueInMmol, toUnit)
    }
}
