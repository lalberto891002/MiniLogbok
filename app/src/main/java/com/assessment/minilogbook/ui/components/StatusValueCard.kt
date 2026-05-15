package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.assessment.minilogbook.R
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme

/**
 * A generic card that displays a labelled value with a colour-coded border and background.
 * The caller is responsible for formatting [label] and [value] as display-ready strings
 * and for resolving the [color] to use.
 *
 * @param label    The title shown above the value (e.g. "Average Blood Glucose").
 * @param value    The already-formatted value string (e.g. "5.40 mmol/L").
 * @param color    The accent colour applied to the border, background tint and text.
 * @param modifier Optional [Modifier] applied to the card.
 */
@Composable
fun StatusValueCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = BorderStroke(2.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Preview(showBackground = true, name = "StatusValueCard – green")
@Composable
private fun StatusValueCardGreenPreview() {
    MiniLogbookTheme {
        StatusValueCard(label = "Average Blood Glucose", value = "5.40 mmol/L", color = Color(0xFF2E7D32))
    }
}

@Preview(showBackground = true, name = "StatusValueCard – red")
@Composable
private fun StatusValueCardRedPreview() {
    MiniLogbookTheme {
        StatusValueCard(label = "Average Blood Glucose", value = "14.80 mmol/L", color = Color(0xFFB71C1C))
    }
}
