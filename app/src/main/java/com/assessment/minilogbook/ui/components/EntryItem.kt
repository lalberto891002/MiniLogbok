package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.assessment.minilogbook.R
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme
import com.assessment.minilogbook.ui.util.getColorForStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A composable item that displays a single glucose entry.
 *
 * @param value The glucose value already converted to the current unit.
 * @param unit The unit of the displayed value, used to format the label.
 * @param status The pre-computed glucose status, provided by the caller (ViewModel).
 * @param timestamp The timestamp of the entry.
 * @param onDelete Called when the user taps the delete icon button.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun EntryItem(
    value: Double,
    unit: GlucoseUnit,
    status: BloodGlucoseStatus,
    timestamp: Long,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { } // Optional click handler for the entire card, e.g. to navigate to detail screen
) {
    val statusColor = getColorForStatus(status)

    val unitLabel = if (unit == GlucoseUnit.MMOL_L) stringResource(R.string.unit_mmol_l) else stringResource(R.string.unit_mg_dl)
    val formattedValue = remember(value, unit) {
        String.format(Locale.US, "%.2f %s", value, unitLabel)
    }

    val formattedDate = remember(timestamp) {
        DateTimeFormatter
            .ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(timestamp))
    }

    Card(
        onClick = onClick,
        modifier = modifier.testTag("entry_$formattedValue"),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation)),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f) // Light background based on status
        )
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.padding_medium)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "EntryItem – in target")
@Composable
private fun EntryItemInTargetPreview() {
    MiniLogbookTheme {
        EntryItem(
            value = 5.40,
            unit = GlucoseUnit.MMOL_L,
            status = BloodGlucoseStatus.IN_TARGET,
            timestamp = 1_715_000_000_000L,
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "EntryItem – out of range")
@Composable
private fun EntryItemOutOfRangePreview() {
    MiniLogbookTheme {
        EntryItem(
            value = 266.4,
            unit = GlucoseUnit.MG_DL,
            status = BloodGlucoseStatus.OUT_OF_RANGE,
            timestamp = 1_715_003_600_000L,
            onDelete = {}
        )
    }
}

