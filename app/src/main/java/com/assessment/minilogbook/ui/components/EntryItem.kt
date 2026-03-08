package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.assessment.minilogbook.R
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.usecase.GlucoseConverter
import com.assessment.minilogbook.ui.util.getColorForStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * A composable item that displays a single glucose entry.
 *
 * @param value The glucose value already converted to the current unit.
 * @param unit The unit of the displayed value, used to determine the glucose status color.
 * @param timestamp The timestamp of the entry.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun EntryItem(
    value: Double,
    unit: GlucoseUnit,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    val converter = remember { GlucoseConverter() }

    // Memoize the glucose status using the already-converted value and its unit.
    // Only recalculated when value or unit changes.
    val status = remember(value, unit) { converter.getGlucoseStatusByUnit(value, unit) }
    val statusColor = getColorForStatus(status)

    // Only recalculated when value or unit label changes, avoiding redundant string formatting.
    val unitLabel = if (unit == GlucoseUnit.MMOL_L) "mmol/L" else "mg/dL"
    val formattedValue = remember(value, unit) {
        String.format(Locale.getDefault(), "%.2f %s", value, unitLabel)
    }

    val formattedDate = remember(timestamp) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    }

    Card(
        modifier = modifier,
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
            Column {
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
        }
    }
}
