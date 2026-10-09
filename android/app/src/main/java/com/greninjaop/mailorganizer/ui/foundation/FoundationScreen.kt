package com.greninjaop.mailorganizer.ui.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Initial application screen — Phase 1 (phase-01 §26).
 *
 * Communicates "Mail Organizer" and the product direction. It is explicitly
 * a foundation build: no Gmail connection, no emails, no classifications,
 * no fake data of any kind (phase-01 §34). The destination list proves the
 * navigation foundation works; each destination is an honest placeholder
 * until its phase builds it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoundationScreen(
    onNavigate: (String) -> Unit,
    viewModel: FoundationViewModel = viewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mail Organizer") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(MoSpacing.xxl))

            Text(
                text = "Mail Organizer",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MoSpacing.sm))
            Text(
                text = "Organize your Gmail into a clearer, action-first workspace.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MoSpacing.sm))
            Text(
                text = "Foundation build · Phase 1 — no account connected yet",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(MoSpacing.xl))
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(MoSpacing.sm))
            SingleChoiceSegmentedButtonRow {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                        label = {
                            Text(
                                mode.name.lowercase()
                                    .replaceFirstChar { it.titlecase() },
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(MoSpacing.xl))
            Text(
                text = "Destinations",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(MoSpacing.xs))
            Text(
                text = "Navigation foundation — screens arrive with their phases.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MoSpacing.sm))

            DestinationSection(
                heading = "Primary",
                routes = AppDestinations.primary,
                onNavigate = onNavigate,
            )
            Spacer(Modifier.height(MoSpacing.md))
            DestinationSection(
                heading = "Secondary",
                routes = AppDestinations.secondary,
                onNavigate = onNavigate,
            )
            Spacer(Modifier.height(MoSpacing.xxl))
        }
    }
}

@Composable
private fun DestinationSection(
    heading: String,
    routes: List<String>,
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        routes.forEach { route ->
            Button(
                onClick = { onNavigate(route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MoSpacing.huge),
            ) {
                Text(AppDestinations.titleFor(route))
            }
        }
    }
}
