package com.assessment.minilogbook.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.tooling.preview.Preview
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme
import com.assessment.minilogbook.ui.viewmodel.GlucoseListEntryUi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Wraps a single [GlucoseListEntryUi] row in a swipe-to-dismiss container.
 *
 * When the user swipes from end-to-start, the background turns red and
 * [onDeleteRequest] is invoked with the entry and a lambda that resets the
 * dismiss state (used for undo via Snackbar).
 *
 * @param entry           The item to display.
 * @param dismissState    The [SwipeToDismissBoxState] that controls the gesture; callers may
 *                        provide their own so that they can reset it externally.
 * @param onDeleteRequest Callback invoked once the swipe threshold is reached.
 * @param onEntryClick    Callback invoked when the row is tapped.
 * @param modifier        Optional [Modifier] applied to the outer [SwipeToDismissBox].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableEntryItem(
    entry: GlucoseListEntryUi,
    dismissState: SwipeToDismissBoxState,
    onDeleteRequest: (Pair<GlucoseListEntryUi, suspend () -> Unit>) -> Unit,
    onEntryClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val isDismissed by remember {
        derivedStateOf { dismissState.currentValue == SwipeToDismissBoxValue.EndToStart }
    }

    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.currentValue }
            .filter { it == SwipeToDismissBoxValue.EndToStart }
            .collect {
                onDeleteRequest(entry to dismissState::reset)
            }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val errorColor = MaterialTheme.colorScheme.errorContainer
            val surfaceColor = MaterialTheme.colorScheme.surface
            val animatable = remember(surfaceColor) { Animatable(surfaceColor) }

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
            value = entry.convertedValue,
            unit = entry.unit,
            status = entry.status,
            timestamp = entry.timestamp,
            onClick = { onEntryClick(entry.id) },
            onDelete = {
                coroutineScope.launch {
                    dismissState.dismiss(SwipeToDismissBoxValue.EndToStart)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "SwipeableEntryItem")
@Composable
private fun SwipeableEntryItemPreview() {
    MiniLogbookTheme {
        val fakeEntry = GlucoseListEntryUi(
            id = 1,
            valueInMmol = 5.4,
            convertedValue = 5.4,
            status = BloodGlucoseStatus.IN_TARGET,
            timestamp = 1_715_000_000_000L,
            unit = GlucoseUnit.MMOL_L
        )
        SwipeableEntryItem(
            entry = fakeEntry,
            dismissState = rememberSwipeToDismissBoxState(),
            onDeleteRequest = {},
            onEntryClick = {}
        )
    }
}



