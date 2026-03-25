package com.assessment.minilogbook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.assessment.minilogbook.ui.screen.GlucoseDetailScreen
import com.assessment.minilogbook.ui.screen.MiniLogbookScreen
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LogbookList.route
    ) {
        composable(route = AppRoutes.LogbookList.route) {
            val viewModel: GlucoseViewModel = koinViewModel()
            MiniLogbookScreen(
                viewModel = viewModel,
                onEntryClick = { entryId ->
                    navController.navigate(AppRoutes.GlucoseDetail.createRoute(entryId))
                }
            )
        }

        composable(
            route = AppRoutes.GlucoseDetail.route,
            arguments = listOf(
                navArgument(AppRoutes.GlucoseDetail.ARG_ENTRY_ID) {
                    type = NavType.IntType
                }
            )
        ) {
            // GlucoseDetailViewModel is created here with its own SavedStateHandle
            // that contains entryId — injected automatically by Koin
            GlucoseDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

