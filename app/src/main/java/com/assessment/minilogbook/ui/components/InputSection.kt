package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.assessment.minilogbook.R
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme

/**
 * Groups the unit selector, glucose input field, and save button into a single section.
 *
 * @param unit                 Currently selected [GlucoseUnit].
 * @param inputValue           Current text in the input field.
 * @param displayErrorMessage  When true, the input field shows an error message.
 * @param onUnitSelected       Callback invoked when the user switches units.
 * @param onValueChange        Callback invoked on every keystroke in the input field.
 * @param onSave               Callback invoked when the user taps the Save button or the keyboard Done action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InputSection(
    unit: GlucoseUnit,
    inputValue: String,
    displayErrorMessage: Boolean,
    onUnitSelected: (GlucoseUnit) -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val unitText = if (unit == GlucoseUnit.MMOL_L)
        stringResource(R.string.unit_mmol_l)
    else
        stringResource(R.string.unit_mg_dl)

    GlucoseUnitSelector(
        selectedUnit = unit,
        onUnitSelected = onUnitSelected,
        modifier = Modifier.fillMaxWidth()
    )

    GlucoseInputField(
        modifier = Modifier.fillMaxWidth(),
        value = inputValue,
        onValueChange = onValueChange,
        unitText = unitText,
        onDone = onSave,
        errorMessage = if (displayErrorMessage) stringResource(R.string.error_invalid_value) else null
    )

    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.action_save))
    }
}

@Preview(showBackground = true, name = "InputSection – mmol/L")
@Composable
private fun InputSectionMmolPreview() {
    MiniLogbookTheme {
        InputSection(
            unit = GlucoseUnit.MMOL_L,
            inputValue = "5.4",
            displayErrorMessage = false,
            onUnitSelected = {},
            onValueChange = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "InputSection – error state")
@Composable
private fun InputSectionErrorPreview() {
    MiniLogbookTheme {
        InputSection(
            unit = GlucoseUnit.MG_DL,
            inputValue = "999",
            displayErrorMessage = true,
            onUnitSelected = {},
            onValueChange = {},
            onSave = {}
        )
    }
}

