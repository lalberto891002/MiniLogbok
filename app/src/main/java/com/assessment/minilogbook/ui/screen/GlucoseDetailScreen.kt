package com.assessment.minilogbook.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.assessment.minilogbook.R
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.ui.components.GlucoseDetailCard
import com.assessment.minilogbook.ui.util.getColorForStatus
import com.assessment.minilogbook.ui.viewmodel.GlucoseDetailViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

        val currentEntry = entry!!

        // Compute values once
        val mmolValue = currentEntry.valueInMmol
        val mgdlValue = remember(mmolValue) { viewModel.convertValue(mmolValue, GlucoseUnit.MG_DL) }
        val status = remember(mmolValue) { viewModel.getStatus(mmolValue) }
        val statusColor = getColorForStatus(status)

        val formattedMmol = remember(mmolValue) {
            String.format(Locale.US, "%.2f %s", mmolValue, "mmol/L")
        }
        val formattedMgdl = remember(mgdlValue) {
            String.format(Locale.US, "%.1f %s", mgdlValue, "mg/dL")
        }
        val formattedDate = remember(currentEntry.timestamp) {
            DateTimeFormatter
                .ofPattern("EEEE, MMM dd yyyy  •  HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(currentEntry.timestamp))
        }
        val statusLabel = when (status) {
            BloodGlucoseStatus.IN_TARGET -> stringResource(R.string.status_in_target)
            BloodGlucoseStatus.OK -> stringResource(R.string.status_ok)
            BloodGlucoseStatus.OUT_OF_RANGE -> stringResource(R.string.status_out_of_range)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(dimensionResource(R.dimen.padding_medium))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
        ) {
            // Primary value card (mmol/L)
            GlucoseDetailCard(
                label = stringResource(R.string.label_glucose_value),
                value = formattedMmol,
                color = statusColor,
                icon = Icons.Filled.Star
            )

            // Secondary value (mg/dL)
            GlucoseDetailCard(
                label = stringResource(R.string.label_glucose_value),
                value = formattedMgdl,
                color = statusColor,
                icon = Icons.Filled.Star
            )

            // Date & time
            GlucoseDetailCard(
                label = stringResource(R.string.label_date_time),
                value = formattedDate,
                color = MaterialTheme.colorScheme.onSurface,
                icon = Icons.Filled.DateRange
            )

            // Status
            GlucoseDetailCard(
                label = stringResource(R.string.label_status),
                value = statusLabel,
                color = statusColor,
                icon = Icons.Filled.Info
            )
        }
    }
}

