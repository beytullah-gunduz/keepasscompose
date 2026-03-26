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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.common.PassphraseGenerator
import org.github.keepasscompose.core.common.PasswordGenerator
import org.github.keepasscompose.core.common.PasswordStrength

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasswordGeneratorScreen(
    onApply: ((String) -> Unit)? = null,
    onCopy: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Password mode state
    var passwordLength by remember { mutableFloatStateOf(20f) }
    var useUppercase by remember { mutableStateOf(true) }
    var useLowercase by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSpecial by remember { mutableStateOf(true) }
    var useBrackets by remember { mutableStateOf(false) }
    var useQuotes by remember { mutableStateOf(false) }
    var useDashes by remember { mutableStateOf(true) }
    var useMath by remember { mutableStateOf(false) }
    var excludeAmbiguous by remember { mutableStateOf(false) }

    // Passphrase mode state
    var wordCount by remember { mutableFloatStateOf(5f) }
    var separator by remember { mutableStateOf(PassphraseGenerator.Separator.DASH) }
    var capitalize by remember { mutableStateOf(false) }
    var includeNumber by remember { mutableStateOf(false) }

    // Generate initial password
    var generatedPassword by remember {
        mutableStateOf(PasswordGenerator.generate(PasswordGenerator.Config()))
    }

    fun regenerate() {
        generatedPassword = if (selectedTab == 0) {
            val config = PasswordGenerator.Config(
                length = passwordLength.toInt(),
                useUppercase = useUppercase,
                useLowercase = useLowercase,
                useDigits = useDigits,
                useSpecial = useSpecial,
                useBrackets = useBrackets,
                useQuotes = useQuotes,
                useDashes = useDashes,
                useMath = useMath,
                excludeAmbiguous = excludeAmbiguous,
            )
            try {
                PasswordGenerator.generate(config)
            } catch (_: IllegalArgumentException) {
                ""
            }
        } else {
            val config = PassphraseGenerator.Config(
                wordCount = wordCount.toInt(),
                separator = separator,
                capitalize = capitalize,
                includeNumber = includeNumber,
            )
            PassphraseGenerator.generate(config)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Password Generator", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Generated password display
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = generatedPassword.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            IconButton(onClick = { onCopy(generatedPassword) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = { regenerate() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Regenerate")
            }
        }

        // Strength indicator
        if (generatedPassword.isNotEmpty()) {
            val strength = remember(generatedPassword) {
                PasswordStrength.estimate(generatedPassword)
            }
            Spacer(modifier = Modifier.height(4.dp))
            val strengthFraction = when (strength.level) {
                PasswordStrength.Level.VERY_WEAK -> 0.1f
                PasswordStrength.Level.WEAK -> 0.3f
                PasswordStrength.Level.FAIR -> 0.5f
                PasswordStrength.Level.STRONG -> 0.75f
                PasswordStrength.Level.VERY_STRONG -> 1.0f
            }
            val strengthColor = when (strength.level) {
                PasswordStrength.Level.VERY_WEAK, PasswordStrength.Level.WEAK -> MaterialTheme.colorScheme.error
                PasswordStrength.Level.FAIR -> MaterialTheme.colorScheme.tertiary
                PasswordStrength.Level.STRONG, PasswordStrength.Level.VERY_STRONG -> MaterialTheme.colorScheme.primary
            }
            LinearProgressIndicator(
                progress = { strengthFraction },
                modifier = Modifier.fillMaxWidth(),
                color = strengthColor,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = strength.level.name.replace('_', ' ').lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = strengthColor,
                )
                Text(
                    text = "Crack time: ${strength.crackTimeDisplay}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password / Passphrase tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; regenerate() }) {
                Text("Password", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; regenerate() }) {
                Text("Passphrase", modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Password options
            Text("Length: ${passwordLength.toInt()}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = passwordLength,
                onValueChange = { passwordLength = it; regenerate() },
                valueRange = 4f..128f,
                steps = 123,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Character Sets", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CharsetChip("A-Z", useUppercase) { useUppercase = it; regenerate() }
                CharsetChip("a-z", useLowercase) { useLowercase = it; regenerate() }
                CharsetChip("0-9", useDigits) { useDigits = it; regenerate() }
                CharsetChip("!@#\$%", useSpecial) { useSpecial = it; regenerate() }
                CharsetChip("()[]{}",useBrackets) { useBrackets = it; regenerate() }
                CharsetChip("\"'`", useQuotes) { useQuotes = it; regenerate() }
                CharsetChip("-_", useDashes) { useDashes = it; regenerate() }
                CharsetChip("+=/|~", useMath) { useMath = it; regenerate() }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = excludeAmbiguous, onCheckedChange = { excludeAmbiguous = it; regenerate() })
                Text("Exclude ambiguous characters (0O1lI|)", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            // Passphrase options
            Text("Word Count: ${wordCount.toInt()}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = wordCount,
                onValueChange = { wordCount = it; regenerate() },
                valueRange = 3f..10f,
                steps = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Separator", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PassphraseGenerator.Separator.entries.forEach { sep ->
                    val label = when (sep) {
                        PassphraseGenerator.Separator.DASH -> "Dash (-)"
                        PassphraseGenerator.Separator.SPACE -> "Space"
                        PassphraseGenerator.Separator.DOT -> "Dot (.)"
                        PassphraseGenerator.Separator.UNDERSCORE -> "Underscore (_)"
                        PassphraseGenerator.Separator.COMMA -> "Comma (,)"
                        PassphraseGenerator.Separator.NONE -> "None"
                    }
                    FilterChip(
                        selected = separator == sep,
                        onClick = { separator = sep; regenerate() },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = capitalize, onCheckedChange = { capitalize = it; regenerate() })
                Text("Capitalize words", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeNumber, onCheckedChange = { includeNumber = it; regenerate() })
                Text("Include number", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Apply button (shown when opened from entry editor)
        if (onApply != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onApply(generatedPassword) },
                enabled = generatedPassword.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun CharsetChip(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label) },
    )
}
