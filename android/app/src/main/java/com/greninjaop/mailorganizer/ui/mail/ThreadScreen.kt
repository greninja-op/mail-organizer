package com.greninjaop.mailorganizer.ui.mail

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.MoLoadingState

/**
 * Conversation/thread view (Phase 6, phase §18–19).
 *
 * Thread ordering: oldest message first, newest last (documented Gmail
 * convention). The newest message starts expanded; older ones expand inline
 * — no separate screens just to read a thread. [focusMessageId] supports
 * deep links to a specific message (§37): it is scrolled to and expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    viewModel: ThreadViewModel,
    focusMessageId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Deep-link focus: expand the target message and scroll to it.
    LaunchedEffect(focusMessageId) {
        focusMessageId?.let { viewModel.ensureExpanded(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to mail")
                    }
                },
                title = {
                    Text(
                        text = MailFormatting.subjectDisplay(subject),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        when (val s = state) {
            ThreadDetailState.Loading -> MoLoadingState(
                message = "Loading thread…",
                modifier = Modifier.padding(padding),
            )
            ThreadDetailState.Empty -> MoEmptyState(
                title = "Thread not found",
                message = "This conversation may have been removed.",
                modifier = Modifier.padding(padding),
            )
            is ThreadDetailState.Content -> {
                LaunchedEffect(focusMessageId, s.messages) {
                    val target = focusMessageId ?: return@LaunchedEffect
                    val index = s.messages.indexOfFirst { it.messageId == target }
                    if (index >= 0) listState.scrollToItem(index)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    items(s.messages, key = { it.messageId }) { message ->
                        MessageCard(
                            message = message,
                            expanded = s.expandedIds.contains(message.messageId),
                            onToggleExpanded = { viewModel.toggleExpanded(message.messageId) },
                            onOpenLink = { url ->
                                openExternalLink(context, url)
                            },
                            classification = s.classifications[message.messageId],
                            priority = s.priorities[message.messageId],
                        )
                    }
                }
            }
        }
    }
}

/**
 * Opens a link in the external browser (phase §25). Only http/https — the
 * URL is re-validated here even though the renderer already filtered it.
 */
private fun openExternalLink(context: android.content.Context, url: String) {
    val scheme = url.substringBefore(':').lowercase()
    if (!url.contains(':') || (scheme != "http" && scheme != "https")) return
    if (url.any { it.isWhitespace() || it.code < 0x20 }) return
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
