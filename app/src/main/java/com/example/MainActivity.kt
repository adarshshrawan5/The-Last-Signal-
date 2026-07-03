package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.CompanionDashboard
import com.example.ui.CompanionViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: CompanionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the Companion ViewModel
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[CompanionViewModel::class.java]

        setContent {
            MyApplicationTheme {
                CompanionDashboard(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Resume atmospheric dread drone automatically
        viewModel.synthesizer.startAmbientDrone()
    }

    override fun onStop() {
        super.onStop()
        // Safely pause audio synthesis in background
        viewModel.synthesizer.stop()
    }
}
