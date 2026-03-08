package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.assessment.minilogbook.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * A stable composable item that displays a single glucose entry.
 * Being marked as [Stable] helps the Compose compiler optimize recompositions.
 *
 * @param value The glucose value already converted to the current unit.
 * @param unitText The text representing the current unit.
 * @param timestamp The timestamp of the entry.
 * @param modifier The modifier to be applied to the layout.
 */
@Stable
@Composable
fun EntryItem(
    value: Double,
    unitText: String,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    // We use remember with value and unitText as keys to ensure the formatted string
    // is only recalculated when these inputs change. This avoids redundant string formatting
    // and helps Compose skip recomposition if the result remains the same.
    val formattedValue = remember(value, unitText) {
        String.format(Locale.getDefault(), "%.2f %s", value, unitText)
    }

    // Encapsulating SimpleDateFormat and Date formatting inside remember(timestamp)
    // prevents the expensive creation of SimpleDateFormat objects and re-parsing
    // on every recomposition, improving list scrolling performance.
    val formattedDate = remember(timestamp) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        dateFormat.format(Date(timestamp))
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation))
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
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
