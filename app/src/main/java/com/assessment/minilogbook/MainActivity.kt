package com.assessment.minilogbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.assessment.minilogbook.ui.screen.MiniLogbookScreen
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniLogbookTheme {
                val viewModel: GlucoseViewModel = koinViewModel()
                MiniLogbookScreen(viewModel = viewModel)
            }
        }
    }
}
