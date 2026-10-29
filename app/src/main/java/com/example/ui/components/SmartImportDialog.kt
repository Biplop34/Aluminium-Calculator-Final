package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.MeasurementSet
import com.example.util.SmartNotepadParser

@Composable
fun SmartImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<MeasurementSet>, Boolean) -> Unit // sets, isAppend
) {
    var rawText by remember { mutableStateOf("") }
    var isAppendMode by remember { mutableStateOf(true) }
    var parseError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("smart_import_dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Notepad Import",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_smart_import_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Paste handwritten notes or typed lines. E.g.:\n• 21/2 x 46 - 45/5\n• 120 x 150 - 149.5\n• 48.4 x 60",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                        parseError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("smart_import_textarea"),
                    placeholder = {
                        Text(
                            "Paste multiple lines here...\n1. 21/2 x 46 - 45/5\n2. 54.5 x 68.2\n3. 119/7 x 120/1 - 120",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )

                if (parseError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = parseError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Example Helper button
                OutlinedButton(
                    onClick = {
                        rawText = "1. 21/2 x 46 - 45/5\n2. 54.5 x 68.2\n3. 119/7 x 120/1 - 120\n4. 48.0 x 60.0"
                        parseError = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("load_example_notes_button")
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load Example Notepad Data")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Import Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mode:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isAppendMode,
                            onClick = { isAppendMode = true },
                            modifier = Modifier.testTag("import_mode_append")
                        )
                        Text("Append", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isAppendMode,
                            onClick = { isAppendMode = false },
                            modifier = Modifier.testTag("import_mode_replace")
                        )
                        Text("Replace All", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsed = SmartNotepadParser.parse(rawText)
                            if (parsed.isEmpty()) {
                                parseError = "No valid measurements found. Make sure lines have 'Height x Length' format."
                            } else {
                                onImport(parsed, isAppendMode)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("parse_import_submit_button")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Sets")
                    }
                }
            }
        }
    }
}
