package com.assessment.minilogbook.ui.screen

import androidx.compose.animation.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.assessment.minilogbook.R
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.ui.components.EntryItem
import com.assessment.minilogbook.ui.components.GlucoseInputField
import com.assessment.minilogbook.ui.components.GlucoseUnitSelector
import com.assessment.minilogbook.ui.components.StatusValueCard
import com.assessment.minilogbook.ui.util.getColorForStatus
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniLogbookScreen(viewModel: GlucoseViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val inputValue by viewModel.inputValue.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val pagedEntries = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    val unit by remember { derivedStateOf { state.unit } }
    val average by remember { derivedStateOf { state.average } }
    val isLoading by remember { derivedStateOf { state.isLoading } }

    val keyboardController = LocalSoftwareKeyboardController.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val deleteLabel = stringResource(R.string.action_delete)
    val undoLabel = stringResource(R.string.action_undo)

    val onUnitSelected: (GlucoseUnit) -> Unit = { viewModel.onUnitChanged(it) }
    val onValueChange: (String) -> Unit = { viewModel.onInputValueChanged(it) }
    val onSave: () -> Unit = {
        viewModel.saveEntry()
        keyboardController?.hide()
    }

    // Fix: Using a coroutine inside the request handler ensures that multiple deletes
    // are queued correctly in the SnackbarHostState and none are lost due to cancellation.
    val onDeleteRequest: (Pair<GlucoseEntry, suspend () -> Unit>) -> Unit =
        remember {
            { (entry, resetDismiss) ->
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = deleteLabel,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        resetDismiss()
                    } else {
                        viewModel.deleteEntry(entry)
                    }
                }
            }
        }

    val isExpanded = with(density) {
        windowInfo.containerSize.width.toDp() > 600.dp
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_mini_logbook)) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.padding_medium)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_large))
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
                ) {
                    InputSection(
                        unit = unit,
                        inputValue = inputValue,
                        errorMessage = errorMessage,
                        onUnitSelected = onUnitSelected,
                        onValueChange = onValueChange,
                        onSave = onSave
                    )
                    SummarySection(average, unit, viewModel)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
                ) {
                    HistorySection(
                        pagedEntries = pagedEntries,
                        unit = unit,
                        viewModel = viewModel,
                        onDeleteRequest = onDeleteRequest
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(dimensionResource(R.dimen.padding_medium)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
            ) {
                InputSection(
                    unit = unit,
                    inputValue = inputValue,
                    errorMessage = errorMessage,
                    onUnitSelected = onUnitSelected,
                    onValueChange = onValueChange,
                    onSave = onSave
                )
                SummarySection(average, unit, viewModel)
                HistorySection(
                    pagedEntries = pagedEntries,
                    unit = unit,
                    viewModel = viewModel,
                    onDeleteRequest = onDeleteRequest
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputSection(
    unit: GlucoseUnit,
    inputValue: String,
    errorMessage: String?,
    onUnitSelected: (GlucoseUnit) -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val unitText = if (unit == GlucoseUnit.MMOL_L)
        stringResource(R.string.unit_mmol_l)
    else
        stringResource(R.string.unit_mg_dl)

    GlucoseUnitSelector(
        selectedUnit = unit,
        onUnitSelected = onUnitSelected,
        modifier = Modifier.fillMaxWidth()
    )

    GlucoseInputField(
        modifier = Modifier.fillMaxWidth(),
        value = inputValue,
        onValueChange = onValueChange,
        unitText = unitText,
        onDone = onSave,
        errorMessage = if (errorMessage != null) stringResource(R.string.error_invalid_value) else null
    )

    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.action_save))
    }
}

@Composable
private fun SummarySection(average: Double, unit: GlucoseUnit, viewModel: GlucoseViewModel) {
    val avgInMmol =  viewModel.convertValue(average, GlucoseUnit.MMOL_L)
    val status = viewModel.getGlucoseStatus(avgInMmol)
    val unitLabel =
        if (unit == GlucoseUnit.MMOL_L) stringResource(R.string.unit_mmol_l) else stringResource(R.string.unit_mg_dl)

    StatusValueCard(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.label_average_bg),
        value = "${String.format(Locale.getDefault(), "%.2f", average)} $unitLabel",
        color = getColorForStatus(status)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySection(
    pagedEntries: LazyPagingItems<GlucoseEntry>,
    unit: GlucoseUnit,
    viewModel: GlucoseViewModel,
    onDeleteRequest: (Pair<GlucoseEntry, suspend () -> Unit>) -> Unit
) {
    Text(
        stringResource(R.string.label_previous_entries),
        style = MaterialTheme.typography.titleLarge
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.padding_medium))
    ) {
        items(
            count = pagedEntries.itemCount,
            key = { index -> pagedEntries[index]?.id ?: index }
        ) { index ->
            val entry = pagedEntries[index] ?: return@items

            val convertedValue = remember(entry.valueInMmol, unit) {
                viewModel.convertValue(entry.valueInMmol, unit)
            }

            val dismissState = rememberSwipeToDismissBoxState()
            val isDismissed by remember {
                derivedStateOf { dismissState.currentValue == SwipeToDismissBoxValue.EndToStart }
            }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(dismissState) {
                snapshotFlow { dismissState.currentValue }
                    .filter { it == SwipeToDismissBoxValue.EndToStart }
                    .collect {
                        onDeleteRequest(entry to dismissState::reset)
                    }
            }

            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    val errorColor = MaterialTheme.colorScheme.errorContainer
                    val surfaceColor = MaterialTheme.colorScheme.surface
                    val animatable = remember { Animatable(surfaceColor) }

                    LaunchedEffect(isDismissed) {
                        animatable.animateTo(if (isDismissed) errorColor else surfaceColor)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                            .drawBehind { drawRect(animatable.value) }
                    )
                }
            ) {
                EntryItem(
                    modifier = Modifier.fillMaxWidth(),
                    value = convertedValue,
                    unit = unit,
                    timestamp = entry.timestamp,
                    onDelete = {
                        coroutineScope.launch {
                            dismissState.dismiss(SwipeToDismissBoxValue.EndToStart)
                        }
                    }
                )
            }
        }
    }
}
