package com.assessment.minilogbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assessment.minilogbook.data.GlucoseDatabase
import com.assessment.minilogbook.ui.screen.MiniLogbookScreen
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = GlucoseDatabase.getDatabase(this)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GlucoseViewModel(database.glucoseDao()) as T
            }
        }

        enableEdgeToEdge()
        setContent {
            MiniLogbookTheme {
                val viewModel: GlucoseViewModel = viewModel(factory = viewModelFactory)
                MiniLogbookScreen(viewModel = viewModel)
            }
        }
    }
}

