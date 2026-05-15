package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.assessment.minilogbook.R
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme
import com.assessment.minilogbook.ui.util.getColorForStatus
import java.util.Locale

/**
 * Displays the average blood-glucose value as a colour-coded [StatusValueCard].
 *
 * @param average Converted average value expressed in [unit].
 * @param unit    The unit that [average] is expressed in (determines the suffix label).
 * @param status  Pre-computed [BloodGlucoseStatus] for colour coding.
 */
@Composable
internal fun SummarySection(average: Double, unit: GlucoseUnit, status: BloodGlucoseStatus) {
    val unitLabel =
        if (unit == GlucoseUnit.MMOL_L) stringResource(R.string.unit_mmol_l)
        else stringResource(R.string.unit_mg_dl)

    StatusValueCard(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.label_average_bg),
        value = "${String.format(Locale.getDefault(), "%.2f", average)} $unitLabel",
        color = getColorForStatus(status)
    )
}

@Preview(showBackground = true, name = "SummarySection – in target")
@Composable
private fun SummarySectionInTargetPreview() {
    MiniLogbookTheme {
        SummarySection(average = 5.40, unit = GlucoseUnit.MMOL_L, status = BloodGlucoseStatus.IN_TARGET)
    }
}

@Preview(showBackground = true, name = "SummarySection – out of range")
@Composable
private fun SummarySectionOutOfRangePreview() {
    MiniLogbookTheme {
        SummarySection(average = 15.30, unit = GlucoseUnit.MMOL_L, status = BloodGlucoseStatus.OUT_OF_RANGE)
    }
}

