package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Import/Export format descriptor.
 */
data class ImportExportFormat(val id: String, val name: String, val description: String, val icon: ImageVector, val isImport: Boolean)

/**
 * State for the import/export wizard.
 */
sealed interface ImportExportState {
    data object FormatSelection : ImportExportState
    data class FileSelection(val format: ImportExportFormat) : ImportExportState
    data class Preview(val format: ImportExportFormat, val entryCount: Int, val groupCount: Int) : ImportExportState
    data class InProgress(val format: ImportExportFormat, val progress: Float) : ImportExportState
    data class Complete(val format: ImportExportFormat, val entriesProcessed: Int, val groupsProcessed: Int) : ImportExportState
    data class Failed(val format: ImportExportFormat, val error: String) : ImportExportState
}

/**
 * Import/Export wizard UI with step-by-step flow:
 * 1. Format selection
 * 2. File picker
 * 3. Preview
 * 4. Progress
 * 5. Summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    onPickFile: (ImportExportFormat) -> Unit = {},
    onConfirmImport: () -> Unit = {},
    onConfirmExport: () -> Unit = {},
    onBack: () -> Unit = {},
    state: ImportExportState = ImportExportState.FormatSelection,
    onFormatSelected: (ImportExportFormat) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import / Export") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            when (state) {
                is ImportExportState.FormatSelection -> FormatSelectionStep(onFormatSelected)
                is ImportExportState.FileSelection -> FileSelectionStep(state.format, onPickFile)
                is ImportExportState.Preview -> PreviewStep(state, onConfirmImport, onConfirmExport)
                is ImportExportState.InProgress -> ProgressStep(state)
                is ImportExportState.Complete -> CompleteStep(state, onBack)
                is ImportExportState.Failed -> FailedStep(state, onBack)
            }
        }
    }
}

@Composable
private fun FormatSelectionStep(onFormatSelected: (ImportExportFormat) -> Unit) {
    Text("Import", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    importFormats.forEach { format ->
        FormatCard(format, onClick = { onFormatSelected(format) })
        Spacer(modifier = Modifier.height(8.dp))
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

    Text("Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    exportFormats.forEach { format ->
        FormatCard(format, onClick = { onFormatSelected(format) })
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FormatCard(format: ImportExportFormat, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                format.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(format.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    format.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FileSelectionStep(format: ImportExportFormat, onPickFile: (ImportExportFormat) -> Unit) {
    Text(
        if (format.isImport) "Select file to import" else "Choose export location",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Format: ${format.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = { onPickFile(format) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Folder, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (format.isImport) "Choose File" else "Choose Location")
    }
}

@Composable
private fun PreviewStep(state: ImportExportState.Preview, onConfirmImport: () -> Unit, onConfirmExport: () -> Unit) {
    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))
    Text("Ready to ${if (state.format.isImport) "import" else "export"}", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text("${state.entryCount} entries in ${state.groupCount} groups", style = MaterialTheme.typography.bodyLarge)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Format: ${state.format.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    if (!state.format.isImport) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(
                "Warning: Exported data will contain passwords in plaintext.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(onClick = if (state.format.isImport) onConfirmImport else onConfirmExport) {
            Text(if (state.format.isImport) "Import" else "Export")
        }
    }
}

@Composable
private fun ProgressStep(state: ImportExportState.InProgress) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (state.format.isImport) "Importing..." else "Exporting...",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CompleteStep(state: ImportExportState.Complete, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (state.format.isImport) "Import Complete" else "Export Complete",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("${state.entriesProcessed} entries, ${state.groupsProcessed} groups", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDone) { Text("Done") }
    }
}

@Composable
private fun FailedStep(state: ImportExportState.Failed, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Operation Failed", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(state.error, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onDone) { Text("Back") }
    }
}

// --- Format definitions ---

private val importFormats = listOf(
    ImportExportFormat("csv", "CSV", "Comma-separated values (generic)", Icons.Default.TableChart, isImport = true),
    ImportExportFormat("lastpass", "LastPass", "LastPass CSV export", Icons.Default.CloudDownload, isImport = true),
    ImportExportFormat("bitwarden", "Bitwarden", "Bitwarden JSON or CSV export", Icons.Default.CloudDownload, isImport = true),
    ImportExportFormat("1password", "1Password", "1Password CSV or 1PIF export", Icons.Default.CloudDownload, isImport = true),
)

private val exportFormats = listOf(
    ImportExportFormat("csv-export", "CSV", "Export entries as CSV", Icons.Default.TableChart, isImport = false),
    ImportExportFormat("html-export", "HTML", "Export as formatted HTML (for printing)", Icons.Default.Description, isImport = false),
    ImportExportFormat("xml-export", "XML", "Export as KeePass XML (unencrypted)", Icons.Default.CloudUpload, isImport = false),
)
