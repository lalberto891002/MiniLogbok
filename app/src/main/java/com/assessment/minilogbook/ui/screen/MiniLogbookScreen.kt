package com.assessment.minilogbook.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.assessment.minilogbook.R
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.ui.components.HistorySection
import com.assessment.minilogbook.ui.components.InputSection
import com.assessment.minilogbook.ui.components.SummarySection
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme
import com.assessment.minilogbook.ui.viewmodel.GlucoseListEntryUi
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniLogbookScreen(viewModel: GlucoseViewModel, onEntryClick: (Int) -> Unit) {
    val glucoseState by viewModel.glucoseState.collectAsStateWithLifecycle()
    val inputValue by viewModel.inputValue.collectAsStateWithLifecycle()
    val displayErrorMessage by viewModel.displayErrorMessage.collectAsStateWithLifecycle()

    val glucoseEntries = viewModel.glucoseEntries.collectAsLazyPagingItems()
    val unit = glucoseState.unit
    val average = glucoseState.average
    val isLoading = glucoseState.isLoading

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

    val onDeleteRequest: (Pair<GlucoseListEntryUi, suspend () -> Unit>) -> Unit =
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

    val summaryStatus = glucoseState.status

    val isExpanded = with(density) {
        windowInfo.containerSize.width.toDp() > 600.dp
    }

    // rotationKey always increments on every orientation change so the key(rotationKey)
    // wrapping HistorySection always gets a fresh value — Compose discards the old
    // composition and all dismissStates reset to Settled. The snackbar is also dismissed.
    var rotationKey by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(isExpanded) {
        rotationKey++
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    MiniLogbookScaffold(
        snackbarHostState = snackbarHostState,
        isLoading = isLoading,
        isExpanded = isExpanded,
        rotationKey = rotationKey,
        unit = unit,
        inputValue = inputValue,
        displayErrorMessage = displayErrorMessage,
        average = average,
        summaryStatus = summaryStatus,
        glucoseEntries = glucoseEntries,
        onUnitSelected = onUnitSelected,
        onValueChange = onValueChange,
        onSave = onSave,
        onDeleteRequest = onDeleteRequest,
        onEntryClick = onEntryClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiniLogbookScaffold(
    snackbarHostState: SnackbarHostState,
    isLoading: Boolean,
    isExpanded: Boolean,
    rotationKey: Int,
    unit: GlucoseUnit,
    inputValue: String,
    displayErrorMessage: Boolean,
    average: Double,
    summaryStatus: com.assessment.minilogbook.domain.model.BloodGlucoseStatus,
    glucoseEntries: androidx.paging.compose.LazyPagingItems<GlucoseListEntryUi>,
    onUnitSelected: (GlucoseUnit) -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: (Pair<GlucoseListEntryUi, suspend () -> Unit>) -> Unit,
    onEntryClick: (Int) -> Unit
) {
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
                        displayErrorMessage = displayErrorMessage,
                        onUnitSelected = onUnitSelected,
                        onValueChange = onValueChange,
                        onSave = onSave
                    )
                    SummarySection(average = average, unit = unit, status = summaryStatus)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
                ) {
                    key(rotationKey) {
                        HistorySection(
                            pagedEntries = glucoseEntries,
                            onDeleteRequest = onDeleteRequest,
                            onEntryClick = onEntryClick
                        )
                    }
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
                    displayErrorMessage = displayErrorMessage,
                    onUnitSelected = onUnitSelected,
                    onValueChange = onValueChange,
                    onSave = onSave
                )
                SummarySection(average = average, unit = unit, status = summaryStatus)
                key(rotationKey) {
                    HistorySection(
                        pagedEntries = glucoseEntries,
                        onDeleteRequest = onDeleteRequest,
                        onEntryClick = onEntryClick
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "MiniLogbookScreen – loading")
@Composable
private fun MiniLogbookLoadingPreview() {
    MiniLogbookTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val emptyPaging = flowOf(PagingData.from(emptyList<GlucoseListEntryUi>())).collectAsLazyPagingItems()
        MiniLogbookScaffold(
            snackbarHostState = snackbarHostState,
            isLoading = true,
            isExpanded = false,
            rotationKey = 0,
            unit = GlucoseUnit.MMOL_L,
            inputValue = "",
            displayErrorMessage = false,
            average = 0.0,
            summaryStatus = com.assessment.minilogbook.domain.model.BloodGlucoseStatus.IN_TARGET,
            glucoseEntries = emptyPaging,
            onUnitSelected = {},
            onValueChange = {},
            onSave = {},
            onDeleteRequest = {},
            onEntryClick = {}
        )
    }
}

@Preview(showBackground = true, name = "MiniLogbookScreen – content")
@Composable
private fun MiniLogbookContentPreview() {
    MiniLogbookTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val fakeItems = listOf(
            GlucoseListEntryUi(1, 5.4, 5.4, com.assessment.minilogbook.domain.model.BloodGlucoseStatus.IN_TARGET, 1_715_000_000_000L, GlucoseUnit.MMOL_L),
            GlucoseListEntryUi(2, 7.8, 7.8, com.assessment.minilogbook.domain.model.BloodGlucoseStatus.OK, 1_715_003_600_000L, GlucoseUnit.MMOL_L)
        )
        val pagingItems = flowOf(PagingData.from(fakeItems)).collectAsLazyPagingItems()
        MiniLogbookScaffold(
            snackbarHostState = snackbarHostState,
            isLoading = false,
            isExpanded = false,
            rotationKey = 0,
            unit = GlucoseUnit.MMOL_L,
            inputValue = "5.4",
            displayErrorMessage = false,
            average = 5.4,
            summaryStatus = com.assessment.minilogbook.domain.model.BloodGlucoseStatus.IN_TARGET,
            glucoseEntries = pagingItems,
            onUnitSelected = {},
            onValueChange = {},
            onSave = {},
            onDeleteRequest = {},
            onEntryClick = {}
        )
    }
}
