package com.assessment.minilogbook.ui.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.ui.theme.*

/**
 * Returns the characteristic color for a glucose status, respecting the current theme mode.
 */
@Composable
fun getColorForStatus(status: BloodGlucoseStatus): Color {
    val isDark = isSystemInDarkTheme()
    return when (status) {
        BloodGlucoseStatus.OUT_OF_RANGE -> if (isDark) GlucoseRedDark else GlucoseRed
        BloodGlucoseStatus.OK -> if (isDark) GlucoseOrangeDark else GlucoseOrange
        BloodGlucoseStatus.IN_TARGET -> if (isDark) GlucoseGreenDark else GlucoseGreen
    }
}
