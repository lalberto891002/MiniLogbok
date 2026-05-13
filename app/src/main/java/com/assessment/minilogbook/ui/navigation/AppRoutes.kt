package com.assessment.minilogbook.ui.navigation

/**
 * Defines all navigation destinations in the app.
 */
sealed class AppRoutes(val route: String) {

    /** Main logbook list. */
    data object LogbookList : AppRoutes("logbook_list")

    /** Detail for a single glucose entry. Requires [ARG_ENTRY_ID] as an integer nav argument. */
    data object GlucoseDetail : AppRoutes("glucose_detail/{entryId}") {
        const val ARG_ENTRY_ID = "entryId"

        /** Builds the concrete route string for the given [entryId]. */
        fun createRoute(entryId: Int) = "glucose_detail/$entryId"
    }
}

