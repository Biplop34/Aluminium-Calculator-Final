package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.CalculationResult
import com.example.model.CustomerDetails
import com.example.util.ThaiFabricationMath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportExportDialog(
    customerDetails: CustomerDetails,
    thaiSize: String,
    results: List<CalculationResult>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalGlassArea = remember(results) {
        ThaiFabricationMath.calculateTotalGlassAreaSqFt(results)
    }

    val reportText = remember(customerDetails, thaiSize, results, totalGlassArea) {
        generateReportText(customerDetails, thaiSize, results, totalGlassArea)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp)
                .testTag("report_export_dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fabrication Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_report_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Monospace Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = reportText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Fabrication Report", reportText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("copy_report_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reportText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Fabrication Report")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1f).testTag("share_report_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

private fun generateReportText(
    customer: CustomerDetails,
    thaiSize: String,
    results: List<CalculationResult>,
    totalGlassArea: Double
): String {
    val dateStr = SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale.getDefault()).format(Date())
    val sb = StringBuilder()
    sb.appendLine("========================================")
    sb.appendLine("  THAI ALUMINIUM & GLASS CUTTING REPORT ")
    sb.appendLine("========================================")
    sb.appendLine("Date: $dateStr")
    sb.appendLine("Profile Standard: Thai $thaiSize\"")
    if (customer.customerName.isNotBlank()) sb.appendLine("Client: ${customer.customerName}")
    if (customer.customerPhone.isNotBlank()) sb.appendLine("Phone: ${customer.customerPhone}")
    if (customer.customerLocation.isNotBlank()) sb.appendLine("Site: ${customer.customerLocation}")
    val brand = if (customer.alumBrand == "Others") customer.otherAlumBrand else customer.alumBrand
    sb.appendLine("Aluminium: ${customer.alumColor} | $brand")
    sb.appendLine("Glass Spec: ${customer.glassType} (${customer.glassThickness})")
    sb.appendLine("----------------------------------------")
    sb.appendLine("CUTTING SPECIFICATIONS:")
    sb.appendLine("----------------------------------------")

    results.forEachIndexed { index, res ->
        val unit = if (res.isInchMode) "in.suta" else "cm"
        sb.appendLine("ITEM #${index + 1} - ${res.label}")
        sb.appendLine("  • Frame Height (Outer Side): ${res.outerSide} $unit (x2)")
        sb.appendLine("  • Frame Width  (Outer Top) : ${res.outerTop} $unit (x1)")
        sb.appendLine("  • Frame Width  (Outer Bot) : ${res.outerBottom} $unit (x1)")
        sb.appendLine("  • Lock Profile / Interlock : ${res.lockVal} $unit (x2)")
        sb.appendLine("  • Shutter Top / Bottom     : ${res.shutterTopBottom} $unit (x4)")
        sb.appendLine("  ★ GLASS SIZE: ${res.glassHeightInch} x ${res.glassLengthInch} inches (x${if (res.isOneShutter) 1 else 2})")
        if (res.netSectionHeight.isNotBlank() && res.netSectionLength.isNotBlank()) {
            sb.appendLine("  • Mosquito Net Screen      : ${res.netSectionHeight} x ${res.netSectionLength} $unit")
        }
        sb.appendLine()
    }

    sb.appendLine("----------------------------------------")
    sb.appendLine("HARDWARE REQUIREMENTS:")
    sb.appendLine("----------------------------------------")
    val hw = ThaiFabricationMath.calculateHardware(results, thaiSize)
    hw.items.forEach { item ->
        sb.appendLine("  • ${item.name}: ${item.value}")
        if (item.isMosquitoNet) {
            sb.appendLine("    (Please Cross Check Mosquito Net By Yourself)")
        }
    }

    sb.appendLine("----------------------------------------")
    sb.appendLine("SUMMARY TOTALS:")
    sb.appendLine("  • Total Items: ${results.size}")
    sb.appendLine("  • Est. Glass Area: ${String.format(Locale.US, "%.2f", totalGlassArea)} Sq. Ft.")
    sb.appendLine("========================================")
    sb.appendLine("Generated by Aluminium Calculator")
    return sb.toString()
}
