package com.nexbytes.h7skertool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexbytes.h7skertool.model.CapturedRequest
import com.nexbytes.h7skertool.model.CapturedResponse
import com.nexbytes.h7skertool.ui.theme.*
import com.nexbytes.h7skertool.utils.DecodeUtils
import com.nexbytes.h7skertool.utils.HexUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingDecodeOverlay(
    request: CapturedRequest,
    response: CapturedResponse?,
    onDismiss: () -> Unit,
    onSaveMod: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var tabIdx by remember { mutableIntStateOf(0) }
    var viewMode by remember { mutableIntStateOf(0) }
    var isDecoding by remember { mutableStateOf(false) }
    var decodedResult by remember { mutableStateOf<String?>(null) }
    var decodeError by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var editBody by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var modName by remember { mutableStateOf("") }
    
    // Manual Hex Input State
    var manualHex by remember { mutableStateOf("") }
    var useManualHex by remember { mutableStateOf(false) }

    val http = remember { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build() }

    val currentText = if (tabIdx == 0) request.bodyText else response?.bodyText
    val currentHex = if (tabIdx == 0) request.body?.let { HexUtils.toHexDump(it) } ?: request.bodyHex ?: "" else response?.body?.let { HexUtils.toHexDump(it) } ?: response?.bodyHex ?: ""
    val cleanHex = if (tabIdx == 0) request.body?.let { HexUtils.toCleanHex(it) } ?: request.bodyHex ?: "" else response?.body?.let { HexUtils.toCleanHex(it) } ?: response?.bodyHex ?: ""
    val localDecoded = remember(tabIdx) { currentText?.let { DecodeUtils.prettyPrintJson(it) } ?: currentText ?: "(empty)" }

    fun decodeViaApi() {
        // Use manual hex if enabled, otherwise use cleanHex
        val hexSource = if (useManualHex && manualHex.isNotBlank()) {
            manualHex
        } else {
            cleanHex
        }
        
        // Remove all spaces and newlines
        val hex = hexSource.replace("\\s".toRegex(), "")
        
        if (hex.isEmpty()) {
            decodeError = "No hex data to decode"
            return
        }
        
        isDecoding = true
        decodedResult = null
        decodeError = null
        
        scope.launch {
            try {
                val url = if (tabIdx == 0) "http://node.mrkalpha.tech:19140/request" else "http://node.mrkalpha.tech:19140/response"
                val body = JSONObject().apply { put("hex", hex) }.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val resp = withContext(Dispatchers.IO) { http.newCall(Request.Builder().url(url).post(body).build()).execute() }
                val txt = resp.body?.string() ?: ""
                withContext(Dispatchers.Main) {
                    isDecoding = false
                    if (txt.isNotEmpty()) {
                        try {
                            decodedResult = DecodeUtils.prettyPrintJson(txt)
                        } catch (e: Exception) {
                            decodedResult = txt
                        }
                    } else {
                        decodeError = "Empty response from server"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDecoding = false
                    decodeError = "Decode API error: ${e.message}"
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(0.98f).fillMaxHeight(0.92f).clip(RoundedCornerShape(18.dp)).background(SheetBlack).border(1.dp, DividerGray, RoundedCornerShape(18.dp))) {
            // Drag handle
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), Alignment.Center) { Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(DividerGray)) }
            
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column { 
                    Text("DECODE WINDOW", color = NeonGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(request.endpoint, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // API decode button
                    IconButton(onClick = ::decodeViaApi, modifier = Modifier.size(36.dp)) { 
                        if (isDecoding) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Amber) 
                        else Icon(Icons.Default.Api, null, tint = Amber, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { editBody = currentText ?: ""; editMode = !editMode }, modifier = Modifier.size(36.dp).background(if (editMode) PurpleAccent.copy(0.1f) else Color.Transparent, RoundedCornerShape(8.dp))) { 
                        Icon(if (editMode) Icons.Default.Check else Icons.Default.Edit, null, tint = if (editMode) PurpleAccent else TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { 
                        val txt = when (viewMode) { 
                            1 -> currentHex
                            2 -> decodedResult ?: localDecoded
                            else -> currentText ?: ""
                        }
                        clipboard.setText(AnnotatedString(txt))
                    }, modifier = Modifier.size(36.dp)) { 
                        Icon(Icons.Default.ContentCopy, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) { 
                        Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            HorizontalDivider(color = DividerGray, thickness = 0.5.dp)
            
            // Request / Response tabs
            TabRow(selectedTabIndex = tabIdx, containerColor = SheetBlack, contentColor = NeonGreen, indicator = { tp -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tp[tabIdx]), color = NeonGreen) }) {
                listOf("REQUEST", "RESPONSE").forEachIndexed { i, t ->
                    Tab(selected = tabIdx == i, onClick = { 
                        tabIdx = i
                        decodedResult = null
                        decodeError = null
                        editMode = false
                        useManualHex = false
                        manualHex = ""
                    }, text = { Text(t, fontSize = 11.sp, fontFamily = FontFamily.Monospace) })
                }
            }
            
            // Manual Hex Input Section
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Toggle for manual hex
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = useManualHex,
                        onCheckedChange = { useManualHex = it },
                        colors = CheckboxDefaults.colors(checkedColor = Amber)
                    )
                    Text("Manual Hex", color = if (useManualHex) Amber else TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                
                if (useManualHex) {
                    OutlinedTextField(
                        value = manualHex,
                        onValueChange = { manualHex = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Paste hex with or without spaces", color = TextSecondary, fontSize = 9.sp) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Amber),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber.copy(0.6f),
                            unfocusedBorderColor = DividerGray,
                            focusedTextColor = Amber,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Amber
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = false,
                        maxLines = 2
                    )
                    if (manualHex.isNotBlank()) {
                        IconButton(
                            onClick = { 
                                manualHex = manualHex.replace("\\s".toRegex(), "")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            // View mode chips
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("TEXT", "HEX", "DECODED").forEachIndexed { i, m ->
                    FilterChip(
                        selected = viewMode == i,
                        onClick = { viewMode = i },
                        label = { Text(m, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen.copy(0.12f),
                            selectedLabelColor = NeonGreen,
                            containerColor = ElevatedBlack,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewMode == i,
                            selectedBorderColor = NeonGreen.copy(0.4f),
                            borderColor = DividerGray
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                if (editMode) {
                    TextButton(
                        onClick = { showSaveDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = PurpleAccent)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SAVE MOD", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            
            HorizontalDivider(color = DividerGray, thickness = 0.5.dp)
            
            // Error display
            decodeError?.let { 
                Row(Modifier.fillMaxWidth().background(AlertRed.copy(0.08f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Error, null, tint = AlertRed, modifier = Modifier.size(14.dp))
                    Text(it, color = AlertRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
            
            // Content
            Column(Modifier.weight(1f)) {
                if (editMode) {
                    Column(Modifier.fillMaxSize().padding(10.dp)) {
                        Text("✏️ Edit ${if (tabIdx == 0) "Request" else "Response"}", color = PurpleAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = editBody,
                            onValueChange = { editBody = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextBright),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleAccent,
                                unfocusedBorderColor = DividerGray,
                                focusedTextColor = TextBright,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = PurpleAccent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                } else {
                    val displayContent = when (viewMode) {
                        0 -> currentText ?: "(empty)"
                        1 -> {
                            if (useManualHex && manualHex.isNotBlank()) {
                                // Show formatted hex from manual input
                                val clean = manualHex.replace("\\s".toRegex(), "")
                                clean.chunked(2).chunked(16).joinToString("\n") { chunk ->
                                    chunk.joinToString(" ") { it.padStart(2, '0').uppercase() }
                                }
                            } else {
                                currentHex.ifEmpty { "(no hex)" }
                            }
                        }
                        else -> decodedResult ?: localDecoded
                    }
                    val textColor = when {
                        viewMode == 1 -> Amber.copy(0.9f)
                        viewMode == 2 -> if (tabIdx == 0) NeonGreen.copy(0.9f) else ElectricBlue.copy(0.9f)
                        tabIdx == 0 -> NeonGreen.copy(0.85f)
                        else -> ElectricBlue.copy(0.85f)
                    }
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        Text(displayContent, color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 17.sp, softWrap = true)
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = ElevatedBlack,
            title = { Text("Save Mod", color = PurpleAccent, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = modName,
                    onValueChange = { modName = it },
                    label = { Text("Mod name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = DividerGray,
                        focusedTextColor = TextBright,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (modName.isNotBlank()) {
                            onSaveMod(editBody)
                            showSaveDialog = false
                            modName = ""
                            editMode = false
                        }
                    }
                ) {
                    Text("SAVE", color = PurpleAccent, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; modName = "" }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
