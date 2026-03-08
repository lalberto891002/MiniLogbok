package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.assessment.minilogbook.R

/**
 * A reusable input field for glucose values.
 *
 * @param value The current text value of the input.
 * @param onValueChange Callback when the text value changes.
 * @param unitText The text representing the glucose unit (e.g., "mmol/L").
 * @param onDone Callback when the "Done" action is triggered from the keyboard.
 * @param modifier The modifier to be applied to the layout.
 * @param errorMessage Optional error message to display.
 */
@Composable
fun GlucoseInputField(
    value: String,
    onValueChange: (String) -> Unit,
    unitText: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.label_enter_bg_value)) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone() }
            ),
            isError = errorMessage != null,
            supportingText = { errorMessage?.let { Text(it) } }
        )
        Text(
            text = unitText,
            fontSize = dimensionResource(R.dimen.text_unit_display).value.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
