package org.github.keepasscompose.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class EntryContextMenuCallbacks(
    val onCopyUsername: () -> Unit = {},
    val onCopyPassword: () -> Unit = {},
    val onCopyUrl: () -> Unit = {},
    val onCopyTotp: (() -> Unit)? = null,
    val onEdit: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onOpenUrl: () -> Unit = {},
)

@Composable
fun EntryContextMenu(expanded: Boolean, onDismissRequest: () -> Unit, callbacks: EntryContextMenuCallbacks, modifier: Modifier = Modifier) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text("Copy Username") },
            onClick = {
                callbacks.onCopyUsername()
                onDismissRequest()
            },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Copy Password") },
            onClick = {
                callbacks.onCopyPassword()
                onDismissRequest()
            },
            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Copy URL") },
            onClick = {
                callbacks.onCopyUrl()
                onDismissRequest()
            },
            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
        )
        if (callbacks.onCopyTotp != null) {
            DropdownMenuItem(
                text = { Text("Copy TOTP") },
                onClick = {
                    callbacks.onCopyTotp.invoke()
                    onDismissRequest()
                },
                leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Edit Entry") },
            onClick = {
                callbacks.onEdit()
                onDismissRequest()
            },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Delete Entry") },
            onClick = {
                callbacks.onDelete()
                onDismissRequest()
            },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Open URL") },
            onClick = {
                callbacks.onOpenUrl()
                onDismissRequest()
            },
            leadingIcon = { Icon(Icons.Filled.OpenInBrowser, contentDescription = null) },
        )
    }
}
