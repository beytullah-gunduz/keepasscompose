package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.common.SearchEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    results: List<SearchEngine.SearchResult>,
    query: String,
    onResultClick: (SearchEngine.SearchResult) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Search Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (query.isNotEmpty()) "No results for \"$query\"" else "Enter a search query",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    text = "${results.size} result${if (results.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            items(results, key = { it.entry.uuid }) { result ->
                SearchResultRow(
                    result = result,
                    query = query,
                    onClick = { onResultClick(result) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchEngine.SearchResult, query: String, onClick: () -> Unit) {
    val entry = result.entry
    val matchedField = findMatchedField(entry, query)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Key,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlightMatch(entry.title, query),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entry.userName.isNotEmpty()) {
                    Text(
                        text = entry.userName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (matchedField != null) {
                    Text(
                        text = "Match: $matchedField",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Text(
                text = result.groupPath.joinToString(" / "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun highlightMatch(text: String, query: String) = buildAnnotatedString {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var start = 0
    while (start < text.length) {
        val matchStart = lowerText.indexOf(lowerQuery, start)
        if (matchStart == -1) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, matchStart))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
            append(text.substring(matchStart, matchStart + query.length))
        }
        start = matchStart + query.length
    }
}

private fun findMatchedField(entry: org.github.keepasscompose.core.model.KdbxEntry, query: String): String? {
    if (query.isBlank()) return null
    val lower = query.lowercase()
    if (entry.title.lowercase().contains(lower)) return null // Title is already shown
    if (entry.userName.lowercase().contains(lower)) return "Username"
    if (entry.url.lowercase().contains(lower)) return "URL"
    if (entry.notes.lowercase().contains(lower)) return "Notes"
    if (entry.tags.any { it.lowercase().contains(lower) }) return "Tag"
    for (field in entry.fields) {
        if (!field.isProtected && field.value.lowercase().contains(lower)) return field.key
    }
    return null
}
