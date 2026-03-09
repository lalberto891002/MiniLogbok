package com.assessment.minilogbook.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.usecase.GlucoseConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class GlucoseState(
    val entries: List<GlucoseEntry> = emptyList(),
    val unit: GlucoseUnit = GlucoseUnit.MMOL_L,
    val isLoading: Boolean = true
)

class GlucoseViewModel(
    private val glucoseDao: GlucoseDao,
    private val converter: GlucoseConverter = GlucoseConverter()
) : ViewModel() {

    private val _unit = MutableStateFlow(GlucoseUnit.MMOL_L)
    private val _inputValue = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Exposed separately — collecting these never triggers a recomposition of
    // sections that only read state (entries/unit)
    val inputValue: StateFlow<String> = _inputValue.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // combine only reacts to DB changes or unit toggle — never to keystrokes
    val state: StateFlow<GlucoseState> = combine(
        glucoseDao.getAllEntries(),
        _unit
    ) { entries, unit ->
        GlucoseState(
            entries = entries,
            unit = unit,
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
            converter.convertValue(value, fromUnit, newUnit).toString()
        } else ""

        _unit.value = newUnit
        _inputValue.value = convertedInput
    }

    fun onInputValueChanged(newValue: String) {
        _inputValue.value = newValue
        _errorMessage.value = null
    }

    fun saveEntry() {
        val rawValue = _inputValue.value.toDoubleOrNull()
        val valueInMmol = converter.toMmolIfValid(rawValue, _unit.value)

        if (valueInMmol != null) {
            viewModelScope.launch {
                glucoseDao.insert(GlucoseEntry(valueInMmol = valueInMmol))
                _inputValue.value = ""
                _errorMessage.value = null
            }
        } else {
            _errorMessage.value = "Please enter a valid value >= 0"
        }
    }

    fun getAverage(unit: GlucoseUnit): Double {
        val entries = state.value.entries
        if (entries.isEmpty()) return 0.0
        val avgMmol = entries.map { it.valueInMmol }.average()
        return converter.fromMmol(avgMmol, unit)
    }

    fun convertValue(valueInMmol: Double, toUnit: GlucoseUnit): Double {
        return converter.fromMmol(valueInMmol, toUnit)
    }

    fun deleteEntry(entry: GlucoseEntry) {
        viewModelScope.launch {
            glucoseDao.delete(entry)
        }
    }

    fun getGlucoseStatus(valueInMmol: Double): BloodGlucoseStatus {
        return converter.getGlucoseStatus(valueInMmol)
    }

}
