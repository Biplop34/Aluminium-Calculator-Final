package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalculatorRepository
import com.example.model.Base8HistoryItem
import com.example.ui.components.SutaInfoDialog
import com.example.util.Base8Math
import kotlinx.coroutines.launch

@Composable
fun Base8MathScreen(
    repository: CalculatorRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var display by remember { mutableStateOf("0") }
    var prevParts by remember { mutableStateOf<Int?>(null) }
    var activeOperator by remember { mutableStateOf<String?>(null) }
    var waitingForOperand by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(repository.getBase8History()) }
    var showInfoDialog by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        } catch (_: Exception) {}
    }

    // Auto-normalize if user types .8 or higher
    LaunchedEffect(display) {
        val normalized = Base8Math.normalizeInput(display)
        if (normalized != display) {
            display = normalized
        }
    }

    fun handleDigit(d: String) {
        triggerHaptic()
        if (waitingForOperand) {
            if (activeOperator == null) expression = ""
            display = d
            waitingForOperand = false
        } else {
            display = if (display == "0") d else display + d
        }
    }

    fun handleDot() {
        triggerHaptic()
        if (waitingForOperand) {
            display = "0."
            waitingForOperand = false
            return
        }
        if (!display.contains(".")) {
            display = "$display."
        }
    }

    fun handleBackspace() {
        triggerHaptic()
        if (display.length > 1) {
            display = display.dropLast(1)
            if (display == "-") display = "0"
        } else {
            display = "0"
        }
    }

    fun handleClear() {
        triggerHaptic()
        display = "0"
        prevParts = null
        activeOperator = null
        waitingForOperand = false
        expression = ""
    }

    fun handleOperator(nextOp: String) {
        triggerHaptic()
        val currentParts = Base8Math.toParts(display)
        if (waitingForOperand && activeOperator != null) {
            activeOperator = nextOp
            return
        }

        if (prevParts == null) {
            prevParts = currentParts
            expression = display
        } else if (activeOperator != null) {
            val resultParts = Base8Math.calculate(activeOperator!!, prevParts!!, currentParts)
            prevParts = resultParts
            expression = "$expression $activeOperator $display"
            display = Base8Math.fromParts(resultParts)
        }
        waitingForOperand = true
        activeOperator = nextOp
    }

    fun handleEquals() {
        triggerHaptic()
        val currentParts = Base8Math.toParts(display)
        if (activeOperator != null && prevParts != null) {
            val resultParts = Base8Math.calculate(activeOperator!!, prevParts!!, currentParts)
            val resultStr = Base8Math.fromParts(resultParts)
            val fullExpr = "$expression $activeOperator $display"

            val newItem = Base8HistoryItem(
                expression = fullExpr,
                result = resultStr,
                resultParts = resultParts
            )

            val updatedHistory = history + newItem
            history = updatedHistory
            repository.saveBase8History(updatedHistory)

            display = resultStr
            prevParts = null
            activeOperator = null
            expression = fullExpr
            waitingForOperand = true

            coroutineScope.launch {
                listState.animateScrollToItem(updatedHistory.size - 1)
            }
        }
    }

    // Live preview total
    val livePreviewResult: String? = remember(display, prevParts, activeOperator, waitingForOperand) {
        if (prevParts == null || activeOperator == null) null
        else if (waitingForOperand) Base8Math.fromParts(prevParts!!)
        else {
            val currentParts = Base8Math.toParts(display)
            val res = Base8Math.calculate(activeOperator!!, prevParts!!, currentParts)
            Base8Math.fromParts(res)
        }
    }

    Scaffold(
        modifier = modifier.testTag("base8_math_screen"),
        topBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Calculator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1 Inch = 8 Sutas • Fractional Arithmetic",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                showInfoDialog = true
                            },
                            modifier = Modifier.testTag("suta_info_btn")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Suta Guide", tint = MaterialTheme.colorScheme.primary)
                        }

                        if (history.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    history = emptyList()
                                    repository.clearBase8History()
                                },
                                modifier = Modifier.testTag("clear_base8_history_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // History Tape (Scrollable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        MaterialTheme.shapes.medium
                    )
                    .padding(8.dp)
            ) {
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No calculations tape yet.\nPerform operations like 35.4 + 1.6 =",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(history, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        triggerHaptic()
                                        display = item.result
                                        prevParts = item.resultParts
                                        expression = item.expression
                                        activeOperator = null
                                        waitingForOperand = true
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.expression,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "= ${item.result}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Screen Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Running expression
                    Text(
                        text = if (expression.isNotBlank() && activeOperator != null)
                            "$expression $activeOperator"
                        else if (expression.isNotBlank()) expression
                        else " ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Display
                    Text(
                        text = display,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    // Live preview badge
                    AnimatedVisibility(
                        visible = livePreviewResult != null && !waitingForOperand,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "≈ ${livePreviewResult ?: ""}\"",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Base-8 Calculator Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: C, Backspace, Divide, Multiply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey(text = "C", onClick = { handleClear() }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(icon = Icons.Default.Backspace, onClick = { handleBackspace() }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "÷", onClick = { handleOperator("/") }, modifier = Modifier.weight(1f), isOperator = true, isSelected = activeOperator == "/")
                    CalcKey(text = "×", onClick = { handleOperator("*") }, modifier = Modifier.weight(1f), isOperator = true, isSelected = activeOperator == "*")
                }

                // Row 2: 7, 8, 9, Minus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey(text = "7", onClick = { handleDigit("7") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "8", onClick = { handleDigit("8") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "9", onClick = { handleDigit("9") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "-", onClick = { handleOperator("-") }, modifier = Modifier.weight(1f), isOperator = true, isSelected = activeOperator == "-")
                }

                // Row 3: 4, 5, 6, Plus
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey(text = "4", onClick = { handleDigit("4") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "5", onClick = { handleDigit("5") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "6", onClick = { handleDigit("6") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "+", onClick = { handleOperator("+") }, modifier = Modifier.weight(1f), isOperator = true, isSelected = activeOperator == "+")
                }

                // Row 4: 1, 2, 3, Equal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey(text = "1", onClick = { handleDigit("1") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "2", onClick = { handleDigit("2") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "3", onClick = { handleDigit("3") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "=", onClick = { handleEquals() }, modifier = Modifier.weight(1f), isAccent = true)
                }

                // Row 5: 0, Dot, Guide
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcKey(text = "0", onClick = { handleDigit("0") }, modifier = Modifier.weight(2f))
                    CalcKey(text = ".", onClick = { handleDot() }, modifier = Modifier.weight(1f))
                    CalcKey(text = "8-Suta", onClick = { showInfoDialog = true }, modifier = Modifier.weight(1f), isSpecial = true)
                }
            }
        }
    }

    if (showInfoDialog) {
        SutaInfoDialog(onDismiss = { showInfoDialog = false })
    }
}

@Composable
private fun CalcKey(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isSelected: Boolean = false,
    isSpecial: Boolean = false,
    isAccent: Boolean = false
) {
    val containerColor = when {
        isAccent -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.secondary
        isOperator -> MaterialTheme.colorScheme.secondaryContainer
        isSpecial -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isAccent -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onSecondary
        isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
        isSpecial -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    ElevatedButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .testTag("key_${text ?: "icon"}"),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        } else if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (text.length > 2) 14.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
