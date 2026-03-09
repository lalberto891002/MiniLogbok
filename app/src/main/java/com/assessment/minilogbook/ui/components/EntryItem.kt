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
import com.assessment.minilogbook.R
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.ui.util.getColorForStatus
import java.text.SimpleDateFormat
import java.util.*

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
    modifier: Modifier = Modifier
) {
    val statusColor = getColorForStatus(status)

    val unitLabel = if (unit == GlucoseUnit.MMOL_L) "mmol/L" else "mg/dL"
    val formattedValue = remember(value, unit) {
        String.format(Locale.getDefault(), "%.2f %s", value, unitLabel)
    }

    val formattedDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        .format(Date(timestamp))

    Card(
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
