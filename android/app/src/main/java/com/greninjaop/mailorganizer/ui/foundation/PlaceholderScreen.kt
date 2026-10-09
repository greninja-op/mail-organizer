package com.greninjaop.mailorganizer.ui.foundation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations

/**
 * Honest foundation placeholder for destinations whose real UI belongs to a
 * later phase. It names the destination and the phase that will build it —
 * never fake content, counts, or functionality (phase-01 §34).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    route: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(AppDestinations.titleFor(route)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        MoEmptyState(
            title = AppDestinations.titleFor(route),
            message = "This screen is built in ${AppDestinations.phaseFor(route)}. " +
                "The navigation foundation is real — the feature is not here yet.",
            actionLabel = "Back to Home",
            onAction = onBack,
            modifier = modifier.padding(padding),
        )
    }
}
