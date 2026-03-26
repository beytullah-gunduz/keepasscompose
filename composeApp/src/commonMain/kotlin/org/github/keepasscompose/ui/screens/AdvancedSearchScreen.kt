package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class AdvancedSearchFilter(
    val query: String = "",
    val searchInTitle: Boolean = true,
    val searchInUsername: Boolean = true,
    val searchInUrl: Boolean = true,
    val searchInNotes: Boolean = true,
    val searchInTags: Boolean = true,
    val searchInCustomFields: Boolean = false,
    val filterExpired: Boolean = false,
    val filterExpiringSoon: Boolean = false,
    val filterNeverExpires: Boolean = false,
    val filterHasAttachment: Boolean = false,
    val filterHasTotp: Boolean = false,
    val filterHasUrl: Boolean = false,
    val groupPath: String = "",
    val includeSubgroups: Boolean = true,
    val tagFilter: String = "",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearchScreen(
    initialFilter: AdvancedSearchFilter = AdvancedSearchFilter(),
    onApply: (AdvancedSearchFilter) -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf(initialFilter) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Advanced Search", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Query
        OutlinedTextField(
            value = filter.query,
            onValueChange = { filter = filter.copy(query = it) },
            label = { Text("Search query") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Search in fields
        Spacer(modifier = Modifier.height(16.dp))
        Text("Search in", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = filter.searchInTitle,
                onClick = { filter = filter.copy(searchInTitle = !filter.searchInTitle) },
                label = { Text("Title") },
            )
            FilterChip(
                selected = filter.searchInUsername,
                onClick = { filter = filter.copy(searchInUsername = !filter.searchInUsername) },
                label = { Text("Username") },
            )
            FilterChip(
                selected = filter.searchInUrl,
                onClick = { filter = filter.copy(searchInUrl = !filter.searchInUrl) },
                label = { Text("URL") },
            )
            FilterChip(
                selected = filter.searchInNotes,
                onClick = { filter = filter.copy(searchInNotes = !filter.searchInNotes) },
                label = { Text("Notes") },
            )
            FilterChip(
                selected = filter.searchInTags,
                onClick = { filter = filter.copy(searchInTags = !filter.searchInTags) },
                label = { Text("Tags") },
            )
            FilterChip(
                selected = filter.searchInCustomFields,
                onClick = { filter = filter.copy(searchInCustomFields = !filter.searchInCustomFields) },
                label = { Text("Custom fields") },
            )
        }

        // Group filter
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Group filter", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = filter.groupPath,
            onValueChange = { filter = filter.copy(groupPath = it) },
            label = { Text("Group path (e.g., Internet/Banking)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = filter.includeSubgroups,
                onCheckedChange = { filter = filter.copy(includeSubgroups = it) },
            )
            Text("Include subgroups", style = MaterialTheme.typography.bodyMedium)
        }

        // Tag filter
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = filter.tagFilter,
            onValueChange = { filter = filter.copy(tagFilter = it) },
            label = { Text("Filter by tag") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Expiration filter
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Expiration", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = filter.filterExpired,
                onClick = { filter = filter.copy(filterExpired = !filter.filterExpired) },
                label = { Text("Expired") },
            )
            FilterChip(
                selected = filter.filterExpiringSoon,
                onClick = { filter = filter.copy(filterExpiringSoon = !filter.filterExpiringSoon) },
                label = { Text("Expiring soon") },
            )
            FilterChip(
                selected = filter.filterNeverExpires,
                onClick = { filter = filter.copy(filterNeverExpires = !filter.filterNeverExpires) },
                label = { Text("Never expires") },
            )
        }

        // Entry properties filter
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Entry properties", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = filter.filterHasAttachment,
                onClick = { filter = filter.copy(filterHasAttachment = !filter.filterHasAttachment) },
                label = { Text("Has attachment") },
            )
            FilterChip(
                selected = filter.filterHasTotp,
                onClick = { filter = filter.copy(filterHasTotp = !filter.filterHasTotp) },
                label = { Text("Has TOTP") },
            )
            FilterChip(
                selected = filter.filterHasUrl,
                onClick = { filter = filter.copy(filterHasUrl = !filter.filterHasUrl) },
                label = { Text("Has URL") },
            )
        }

        // Actions
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = { onApply(filter) }) {
                Text("Search")
            }
        }
    }
}
