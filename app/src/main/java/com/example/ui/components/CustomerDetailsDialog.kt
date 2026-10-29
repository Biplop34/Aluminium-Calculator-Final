package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.CustomerDetails
import com.example.model.FabricationConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsDialog(
    initialDetails: CustomerDetails,
    onDismiss: () -> Unit,
    onSave: (CustomerDetails) -> Unit
) {
    var name by remember { mutableStateOf(initialDetails.customerName) }
    var phone by remember { mutableStateOf(initialDetails.customerPhone) }
    var location by remember { mutableStateOf(initialDetails.customerLocation) }
    var alumColor by remember { mutableStateOf(initialDetails.alumColor) }
    var alumBrand by remember { mutableStateOf(initialDetails.alumBrand) }
    var otherBrand by remember { mutableStateOf(initialDetails.otherAlumBrand) }
    var glassType by remember { mutableStateOf(initialDetails.glassType) }
    var glassThickness by remember { mutableStateOf(initialDetails.glassThickness) }

    // Auto-normalize: Clear Glass 5.5mm -> 5mm
    LaunchedEffect(glassType, glassThickness) {
        if (glassType == "Clear Glass" && glassThickness == "5.5 MM") {
            glassThickness = "5 MM"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("customer_details_dialog"),
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
                    Text(
                        text = "Customer & Material Specs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_customer_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Customer Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer / Project Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Site / Job Location") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_location_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Aluminium Color
                Text(
                    text = "Aluminium Profile Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                DropdownMenuSelector(
                    options = FabricationConstants.ALUM_COLORS,
                    selectedOption = alumColor,
                    onSelect = { alumColor = it },
                    label = "Aluminium Color",
                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    modifier = Modifier.testTag("alum_color_selector")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Aluminium Brand
                Text(
                    text = "Aluminium Brand",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                DropdownMenuSelector(
                    options = FabricationConstants.ALUM_BRANDS,
                    selectedOption = alumBrand,
                    onSelect = { alumBrand = it },
                    label = "Aluminium Brand",
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    modifier = Modifier.testTag("alum_brand_selector")
                )

                if (alumBrand == "Others") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otherBrand,
                        onValueChange = { otherBrand = it },
                        label = { Text("Specify Brand Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Glass Type
                Text(
                    text = "Glass Type & Coating",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                DropdownMenuSelector(
                    options = FabricationConstants.GLASS_TYPES,
                    selectedOption = glassType,
                    onSelect = { glassType = it },
                    label = "Glass Type",
                    leadingIcon = { Icon(Icons.Default.Window, contentDescription = null) },
                    modifier = Modifier.testTag("glass_type_selector")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Glass Thickness
                Text(
                    text = "Glass Thickness",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                DropdownMenuSelector(
                    options = FabricationConstants.GLASS_THICKNESSES,
                    selectedOption = glassThickness,
                    onSelect = { glassThickness = it },
                    label = "Glass Thickness",
                    modifier = Modifier.testTag("glass_thickness_selector")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_customer_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                CustomerDetails(
                                    customerName = name.trim(),
                                    customerPhone = phone.trim(),
                                    customerLocation = location.trim(),
                                    alumColor = alumColor,
                                    alumBrand = alumBrand,
                                    otherAlumBrand = otherBrand.trim(),
                                    glassType = glassType,
                                    glassThickness = glassThickness
                                )
                            )
                        },
                        modifier = Modifier.testTag("save_customer_button")
                    ) {
                        Text("Save Details")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuSelector(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    label: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
