package org.github.keepasscompose.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class KdfConfig(val algorithm: String, val iterations: Long, val memoryMb: Int = 64, val parallelism: Int = 2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdfConfigScreen(
    initialConfig: KdfConfig = KdfConfig("Argon2d", 10),
    isBenchmarking: Boolean = false,
    benchmarkResult: String? = null,
    onBenchmark: () -> Unit = {},
    onSave: (KdfConfig) -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var algorithm by remember { mutableStateOf(initialConfig.algorithm) }
    var iterations by remember { mutableStateOf(initialConfig.iterations.toString()) }
    var memoryMb by remember { mutableStateOf(initialConfig.memoryMb.toString()) }
    var parallelism by remember { mutableStateOf(initialConfig.parallelism.toString()) }

    val isArgon2 = algorithm.startsWith("Argon2")

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("KDF Configuration") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The Key Derivation Function slows down brute-force attacks. Higher values are more secure but slower to unlock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // KDF selector
            var expanded by remember { mutableStateOf(false) }
            val options = listOf("AES-KDF", "Argon2d", "Argon2id")
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = algorithm,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("KDF Algorithm") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                algorithm = option
                                expanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Parameters
            if (isArgon2) {
                OutlinedTextField(
                    value = iterations,
                    onValueChange = { if (it.all { c -> c.isDigit() }) iterations = it },
                    label = { Text("Iterations") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = memoryMb,
                    onValueChange = { if (it.all { c -> c.isDigit() }) memoryMb = it },
                    label = { Text("Memory (MB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = parallelism,
                    onValueChange = { if (it.all { c -> c.isDigit() }) parallelism = it },
                    label = { Text("Parallelism") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = iterations,
                    onValueChange = { if (it.all { c -> c.isDigit() }) iterations = it },
                    label = { Text("Transform Rounds") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Benchmark
            OutlinedButton(onClick = onBenchmark, enabled = !isBenchmarking) {
                if (isBenchmarking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Benchmarking...")
                } else {
                    Text("Benchmark (1 second delay)")
                }
            }
            if (benchmarkResult != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = benchmarkResult,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onSave(
                        KdfConfig(
                            algorithm = algorithm,
                            iterations = iterations.toLongOrNull() ?: 10,
                            memoryMb = memoryMb.toIntOrNull() ?: 64,
                            parallelism = parallelism.toIntOrNull() ?: 2,
                        ),
                    )
                }) { Text("Save") }
            }
        }
    }
}
