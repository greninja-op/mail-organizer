package com.greninjaop.mailorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.ui.foundation.FoundationViewModel
import com.greninjaop.mailorganizer.ui.navigation.AppNavGraph
import com.greninjaop.mailorganizer.ui.theme.MailOrganizerTheme

class MainActivity : ComponentActivity() {

    private val foundationViewModel: FoundationViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as MailOrganizerApp
                return FoundationViewModel(
                    themePreferences = app.container.themePreferences,
                    dispatchers = app.container.dispatchers,
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MailOrganizerTheme(themeMode = foundationViewModel.themeMode) {
                AppNavGraph()
            }
        }
    }
}
