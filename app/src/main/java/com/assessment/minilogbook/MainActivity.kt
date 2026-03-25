package com.assessment.minilogbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.assessment.minilogbook.ui.navigation.AppNavGraph
import com.assessment.minilogbook.ui.theme.MiniLogbookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniLogbookTheme {
                AppNavGraph()
            }
        }
    }
}
