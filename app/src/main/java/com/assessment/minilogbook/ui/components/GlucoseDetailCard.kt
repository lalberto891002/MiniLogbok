package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.assessment.minilogbook.R

/**
 * A reusable card that displays a single labelled data field for a glucose entry detail.
 *
 * The card is colour-coded: border and icon tint match [color], and the background is a
 * translucent tint of the same colour, so status (in-target, acceptable, out-of-range)
 * is immediately visible.
 *
 * @param label   Short title describing the field (e.g. "Glucose Value").
 * @param value   The formatted content string to display prominently.
 * @param color   Accent colour derived from [BloodGlucoseStatus] via [getColorForStatus].
 * @param icon    Optional leading icon shown to the left of the label.
 * @param modifier Optional [Modifier] applied to the card.
 */
@Composable
fun GlucoseDetailCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null

) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        border = BorderStroke(2.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

