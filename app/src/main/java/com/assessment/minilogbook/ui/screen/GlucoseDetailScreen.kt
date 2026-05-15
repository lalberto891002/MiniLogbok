package com.assessment.minilogbook.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.assessment.minilogbook.R
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.ui.components.GlucoseDetailCard
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme
import com.assessment.minilogbook.ui.util.getColorForStatus
import com.assessment.minilogbook.ui.viewmodel.GlucoseDetailViewModel
import com.assessment.minilogbook.ui.viewmodel.GlucoseEntryUi
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: GlucoseDetailViewModel = koinViewModel()
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_entry_detail)) },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (entry == null) {
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

        DetailContent(
            entry = entry!!,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

/**
 * Stateless content body of [GlucoseDetailScreen].
 *
 * Renders four [GlucoseDetailCard] items: mmol/L value, mg/dL value, date/time and status.
 * Separated from the screen composable so it can be previewed in isolation.
 */
@Composable
private fun DetailContent(entry: GlucoseEntryUi, modifier: Modifier = Modifier) {
    val statusColor = getColorForStatus(entry.status)

    val formattedMmol = String.format(Locale.US, "%.2f %s", entry.valueInMmol, "mmol/L")
    val formattedMgdl = String.format(Locale.US, "%.1f %s", entry.valueInMgdl, "mg/dL")
    val statusLabel = when (entry.status) {
        BloodGlucoseStatus.IN_TARGET -> stringResource(R.string.status_in_target)
        BloodGlucoseStatus.OK -> stringResource(R.string.status_ok)
        BloodGlucoseStatus.OUT_OF_RANGE -> stringResource(R.string.status_out_of_range)
    }

    Column(
        modifier = modifier
            .padding(dimensionResource(R.dimen.padding_medium))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        GlucoseDetailCard(
            label = stringResource(R.string.label_glucose_value),
            value = formattedMmol,
            color = statusColor,
            icon = Icons.Filled.Star
        )
        GlucoseDetailCard(
            label = stringResource(R.string.label_glucose_value),
            value = formattedMgdl,
            color = statusColor,
            icon = Icons.Filled.Star
        )
        GlucoseDetailCard(
            label = stringResource(R.string.label_date_time),
            value = entry.formattedDate,
            color = MaterialTheme.colorScheme.onSurface,
            icon = Icons.Filled.DateRange
        )
        GlucoseDetailCard(
            label = stringResource(R.string.label_status),
            value = statusLabel,
            color = statusColor,
            icon = Icons.Filled.Info
        )
    }
}

@Preview(showBackground = true, name = "DetailContent – in target")
@Composable
private fun DetailContentInTargetPreview() {
    MiniLogbookTheme {
        DetailContent(
            entry = GlucoseEntryUi(
                id = 1,
                formattedDate = "Thursday, May 15 2026  •  08:30",
                valueInMmol = 5.40,
                valueInMgdl = 97.2,
                status = BloodGlucoseStatus.IN_TARGET
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "DetailContent – out of range")
@Composable
private fun DetailContentOutOfRangePreview() {
    MiniLogbookTheme {
        DetailContent(
            entry = GlucoseEntryUi(
                id = 2,
                formattedDate = "Thursday, May 15 2026  •  14:00",
                valueInMmol = 14.80,
                valueInMgdl = 266.4,
                status = BloodGlucoseStatus.OUT_OF_RANGE
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
