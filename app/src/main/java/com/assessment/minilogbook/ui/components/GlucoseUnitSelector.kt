package com.assessment.minilogbook.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.assessment.minilogbook.R
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme

/**
 * A segmented button row that lets the user switch between glucose units.
 *
 * @param selectedUnit   The currently active [GlucoseUnit].
 * @param onUnitSelected Callback invoked with the newly chosen unit.
 * @param modifier       Optional [Modifier] applied to the row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseUnitSelector(
    selectedUnit: GlucoseUnit,
    onUnitSelected: (GlucoseUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        GlucoseUnit.entries.forEachIndexed { index, unit ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = GlucoseUnit.entries.size
                ),
                onClick = { onUnitSelected(unit) },
                selected = selectedUnit == unit,
                colors = SegmentedButtonDefaults.colors(
                    // Selected state: primary background with on-primary text
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    // Unselected state: transparent background with primary text
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.primary,
                    inactiveBorderColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (unit == GlucoseUnit.MMOL_L)
                        stringResource(R.string.unit_mmol_l)
                    else
                        stringResource(R.string.unit_mg_dl)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "GlucoseUnitSelector – mmol/L selected")
@Composable
private fun GlucoseUnitSelectorMmolPreview() {
    MiniLogbookTheme {
        GlucoseUnitSelector(selectedUnit = GlucoseUnit.MMOL_L, onUnitSelected = {})
    }
}

@Preview(showBackground = true, name = "GlucoseUnitSelector – mg/dL selected")
@Composable
private fun GlucoseUnitSelectorMgdlPreview() {
    MiniLogbookTheme {
        GlucoseUnitSelector(selectedUnit = GlucoseUnit.MG_DL, onUnitSelected = {})
    }
}
