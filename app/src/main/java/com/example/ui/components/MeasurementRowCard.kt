package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.MeasurementSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementRowCard(
    set: MeasurementSet,
    index: Int,
    onUpdate: (MeasurementSet) -> Unit,
    onRemove: () -> Unit,
    onDuplicate: () -> Unit,
    canRemove: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("measurement_set_card_$index"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Badge, Mode Chips, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Item #${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Inch / CM Toggle
                    FilterChip(
                        selected = set.isInchMode,
                        onClick = { onUpdate(set.copy(isInchMode = !set.isInchMode)) },
                        label = { Text(if (set.isInchMode) "INCHES" else "CM") },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("unit_toggle_$index")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Duplicate
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(32.dp).testTag("duplicate_set_$index")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Remove
                    if (canRemove) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(32.dp).testTag("remove_set_$index")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inputs: Height x Length1 - Length2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Height
                OutlinedTextField(
                    value = set.height,
                    onValueChange = { onUpdate(set.copy(height = it)) },
                    label = { Text("Height (${if (set.isInchMode) "In" else "Cm"})") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_height_$index"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Length 1 (Top / Main)
                OutlinedTextField(
                    value = set.length1,
                    onValueChange = { onUpdate(set.copy(length1 = it)) },
                    label = { Text("Length 1 (${if (set.isInchMode) "In" else "Cm"})") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_length1_$index"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Length 2 (Bottom / Optional)
                OutlinedTextField(
                    value = set.length2,
                    onValueChange = { onUpdate(set.copy(length2 = it)) },
                    label = { Text("Length 2") },
                    placeholder = { Text("Opt") },
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("input_length2_$index"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Override Chips: Fan Light & 1-Shutter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = set.isOverridden,
                    onClick = { onUpdate(set.copy(isOverridden = !set.isOverridden)) },
                    label = { Text("Fan Light Override", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("override_fanlight_$index")
                )

                FilterChip(
                    selected = set.isShutterOverridden,
                    onClick = { onUpdate(set.copy(isShutterOverridden = !set.isShutterOverridden)) },
                    label = { Text("1-Shutter Override", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("override_shutter_$index")
                )
            }
        }
    }
}
