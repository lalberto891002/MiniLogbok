package com.assessment.minilogbook.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.assessment.minilogbook.R
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.ui.components.EntryItem
import com.assessment.minilogbook.ui.components.GlucoseInputField
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniLogbookScreen(viewModel: GlucoseViewModel) {
    val state by viewModel.state.collectAsState()
    val average = viewModel.getAverage(state.unit)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val isExpanded = with(density) {
        windowInfo.containerSize.width.toDp() > 600.dp
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_mini_logbook)) })
        }
    ) { padding ->
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.padding_medium)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_large))
            ) {
                // Left Column: Input and Summary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
                ) {
                    InputSection(state, viewModel, keyboardController, focusManager)
                    SummarySection(average, state.unit)
                }

                // Right Column: History
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
                ) {
                    HistorySection(state.entries, state.unit, viewModel)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.padding_medium)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
            ) {
                InputSection(state, viewModel, keyboardController, focusManager)
                SummarySection(average, state.unit)
                HistorySection(state.entries, state.unit, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputSection(
    state: com.assessment.minilogbook.ui.viewmodel.GlucoseState,
    viewModel: GlucoseViewModel,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager
) {
    // Unit Selector
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        GlucoseUnit.entries.forEachIndexed { index, unit ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = GlucoseUnit.entries.size),
                onClick = { viewModel.onUnitChanged(unit) },
                selected = state.unit == unit
            ) {
                Text(if (unit == GlucoseUnit.MMOL_L) stringResource(R.string.unit_mmol_l) else stringResource(R.string.unit_mg_dl))
            }
        }
    }

    // Input Field
    GlucoseInputField(
        modifier = Modifier.fillMaxWidth(),
        value = state.inputValue,
        onValueChange = { viewModel.onInputValueChanged(it) },
        unitText = if (state.unit == GlucoseUnit.MMOL_L) stringResource(R.string.unit_mmol_l) else stringResource(R.string.unit_mg_dl),
        onDone = {
            viewModel.saveEntry()
            keyboardController?.hide()
            focusManager.clearFocus()
        },
        errorMessage = if (state.errorMessage != null) stringResource(R.string.error_invalid_value) else null
    )

    // Save Button
    Button(
        onClick = {
            viewModel.saveEntry()
            keyboardController?.hide()
            focusManager.clearFocus()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.action_save))
    }
}

@Composable
private fun SummarySection(average: Double, unit: GlucoseUnit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.label_average_bg), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${String.format(Locale.getDefault(), "%.2f", average)} ${if (unit == GlucoseUnit.MMOL_L) stringResource(R.string.unit_mmol_l) else stringResource(R.string.unit_mg_dl)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HistorySection(entries: List<GlucoseEntry>, unit: GlucoseUnit, viewModel: GlucoseViewModel) {
    Text(stringResource(R.string.label_previous_entries), style = MaterialTheme.typography.titleLarge)
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.padding_medium))
    ) {
        itemsIndexed(
            items = entries,
            key = { _, entry -> entry.timestamp }
        ) { _, entry ->
            val convertedValue = remember(entry.valueInMmol, unit) {
                viewModel.convertValue(entry.valueInMmol, unit)
            }
            val unitLabel = if (unit == GlucoseUnit.MMOL_L) stringResource(R.string.unit_mmol_l) else stringResource(R.string.unit_mg_dl)

            EntryItem(
                modifier = Modifier.fillMaxWidth(),
                value = convertedValue,
                unitText = unitLabel,
                timestamp = entry.timestamp
            )
        }
    }
}
