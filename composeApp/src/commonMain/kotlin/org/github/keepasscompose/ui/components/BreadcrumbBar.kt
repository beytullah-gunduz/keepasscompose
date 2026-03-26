package org.github.keepasscompose.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxGroup

@Composable
fun BreadcrumbBar(
    breadcrumb: List<KdbxGroup>,
    onGroupClick: (KdbxGroup) -> Unit,
    onNavigateUp: () -> Unit,
    hasParent: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (hasParent) {
            IconButton(onClick = onNavigateUp, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go to parent",
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        breadcrumb.forEachIndexed { index, group ->
            if (index > 0) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val isLast = index == breadcrumb.lastIndex
            Text(
                text = group.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = if (!isLast) {
                    Modifier.clickable { onGroupClick(group) }.padding(horizontal = 4.dp)
                } else {
                    Modifier.padding(horizontal = 4.dp)
                },
            )
        }
    }
}
