package org.github.keepasscompose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class DatabaseTab(
    val id: String,
    val name: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseTabBar(
    tabs: List<DatabaseTab>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = tabs.indexOfFirst { it.id == selectedTabId }.coerceAtLeast(0)

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        divider = { HorizontalDivider() },
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.id == selectedTabId
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab.id) },
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = tab.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        IconButton(
                            onClick = { onTabClosed(tab.id) },
                            modifier = Modifier.size(18.dp),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close ${tab.name}",
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                },
            )
        }

        // "+" tab to open another database
        Tab(
            selected = false,
            onClick = onNewTab,
            content = {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Open Database",
                    modifier = Modifier.padding(8.dp).size(20.dp),
                )
            },
        )
    }
}
