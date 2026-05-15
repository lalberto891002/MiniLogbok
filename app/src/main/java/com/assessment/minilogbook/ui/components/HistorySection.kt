package com.assessment.minilogbook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.assessment.minilogbook.R
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme
import com.assessment.minilogbook.ui.viewmodel.GlucoseListEntryUi
import kotlinx.coroutines.flow.flowOf

/**
 * Renders a header label and a lazy, paged list of glucose entries.
 *
 * Each row is wrapped in a [SwipeableEntryItem] so the user can swipe left to delete.
 * When exactly one new item is appended (i.e. a fresh save), the list auto-scrolls to the top.
 *
 * @param pagedEntries    Paged stream of [GlucoseListEntryUi] items to display.
 * @param onDeleteRequest Callback invoked after a swipe, forwarding the entry and a reset lambda.
 * @param onEntryClick    Callback invoked when the user taps a row.
 * @param listState       Optional external [LazyListState]; a new instance is created by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistorySection(
    pagedEntries: LazyPagingItems<GlucoseListEntryUi>,
    onDeleteRequest: (Pair<GlucoseListEntryUi, suspend () -> Unit>) -> Unit,
    onEntryClick: (Int) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    Text(
        stringResource(R.string.label_previous_entries),
        style = MaterialTheme.typography.titleLarge
    )

    var previousCount by rememberSaveable { mutableIntStateOf(0) }
    val itemCount = pagedEntries.itemCount

    LaunchedEffect(itemCount) {
        // Only auto-scroll when exactly one new item was inserted (not during bulk initial page loads).
        if (itemCount == previousCount + 1 && listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(0)
        }
        previousCount = itemCount
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.padding_medium))
    ) {
        items(
            count = pagedEntries.itemCount,
            key = { index -> pagedEntries.peek(index)?.id ?: index }
        ) { index ->
            val entry = pagedEntries[index] ?: return@items
            val dismissState = rememberSwipeToDismissBoxState()

            SwipeableEntryItem(
                entry = entry,
                dismissState = dismissState,
                onDeleteRequest = onDeleteRequest,
                onEntryClick = onEntryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            )
        }
    }
}

@Preview(showBackground = true, name = "HistorySection – populated")
@Composable
private fun HistorySectionPopulatedPreview() {
    MiniLogbookTheme {
        val fakeItems = listOf(
            GlucoseListEntryUi(1, 5.4, 5.4, BloodGlucoseStatus.IN_TARGET, 1_715_000_000_000L, GlucoseUnit.MMOL_L),
            GlucoseListEntryUi(2, 7.8, 7.8, BloodGlucoseStatus.OK, 1_715_003_600_000L, GlucoseUnit.MMOL_L),
            GlucoseListEntryUi(3, 13.5, 13.5, BloodGlucoseStatus.OUT_OF_RANGE, 1_715_007_200_000L, GlucoseUnit.MMOL_L)
        )
        val pagingItems = flowOf(PagingData.from(fakeItems)).collectAsLazyPagingItems()
        HistorySection(pagedEntries = pagingItems, onDeleteRequest = {}, onEntryClick = {})
    }
}

@Preview(showBackground = true, name = "HistorySection – empty")
@Composable
private fun HistorySectionEmptyPreview() {
    MiniLogbookTheme {
        val pagingItems = flowOf(PagingData.from(emptyList<GlucoseListEntryUi>())).collectAsLazyPagingItems()
        HistorySection(pagedEntries = pagingItems, onDeleteRequest = {}, onEntryClick = {})
    }
}

