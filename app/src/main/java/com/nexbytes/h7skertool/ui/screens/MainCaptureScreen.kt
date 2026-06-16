package com.nexbytes.h7skertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexbytes.h7skertool.model.CapturedRequest
import com.nexbytes.h7skertool.model.CapturedResponse
import com.nexbytes.h7skertool.ui.components.FloatingDecodeOverlay
import com.nexbytes.h7skertool.ui.components.LogViewer
import com.nexbytes.h7skertool.ui.components.UnifiedCaptureItem
import com.nexbytes.h7skertool.ui.theme.*
import com.nexbytes.h7skertool.utils.ExportUtils
import com.nexbytes.h7skertool.utils.ModFile
import com.nexbytes.h7skertool.viewmodel.AppUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCaptureScreen(
    state: AppUiState,
    savedMods: List<ModFile>,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onSearch: (String) -> Unit,
    onFilterEndpoint: (String?) -> Unit,
    onClearCaptures: () -> Unit,
    onSaveMod: (String, String) -> Unit,
    onClearLogs: () -> Unit,
    onNavigateToDetail: (CapturedRequest) -> Unit,
    onSelectMod: (ModFile?) -> Unit,
    selectedMod: ModFile?
) {
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableIntStateOf(0) }
    var decodeTarget by remember { mutableStateOf<CapturedRequest?>(null) }
    var snack by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.isCapturing) NeonGreen else TextDim)); Text("H7skER TOOL", color = NeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp); Text("v3.0", color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace) } },
                    actions = { if (state.requests.isNotEmpty()) { Box(Modifier.clip(RoundedCornerShape(8.dp)).background(NeonGreen.copy(0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("${state.filteredRequests.size}", color = NeonGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(4.dp)) }; IconButton(onClick = onClearCaptures) { Icon(Icons.Default.DeleteSweep, null, tint = Amber) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBlack)
                )
                // Search bar
                OutlinedTextField(value = state.searchQuery, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(46.dp), placeholder = { Text("Search endpoints, bodies…", color = TextDim, fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }, trailingIcon = { if (state.searchQuery.isNotEmpty()) IconButton(onClick = { onSearch("") }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Clear, null, tint = TextSecondary, modifier = Modifier.size(16.dp)) } }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = DividerGray, focusedTextColor = TextBright, unfocusedTextColor = TextPrimary, cursorColor = NeonGreen), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp), shape = RoundedCornerShape(10.dp))
                // Endpoint filter chips
                if (state.allEndpoints.isNotEmpty()) LazyRow(Modifier.fillMaxWidth().background(CardBlack), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = state.endpointFilter == null, onClick = { onFilterEndpoint(null) }, label = { Text("ALL", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonGreen.copy(0.15f), selectedLabelColor = NeonGreen, containerColor = ElevatedBlack, labelColor = TextSecondary), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.endpointFilter == null, selectedBorderColor = NeonGreen.copy(0.4f), borderColor = DividerGray)) }
                    items(state.allEndpoints) { ep -> FilterChip(selected = state.endpointFilter == ep, onClick = { onFilterEndpoint(if (state.endpointFilter == ep) null else ep) }, label = { Text(ep.trimStart('/'), fontSize = 10.sp, fontFamily = FontFamily.Monospace) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricBlue.copy(0.15f), selectedLabelColor = ElectricBlue, containerColor = ElevatedBlack, labelColor = TextSecondary), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.endpointFilter == ep, selectedBorderColor = ElectricBlue.copy(0.4f), borderColor = DividerGray)) }
                }
                // MOD SLIDER — shown before Start button
                if (!state.isCapturing && savedMods.isNotEmpty()) ModSlider(mods = savedMods, selectedMod = selectedMod, onSelectMod = onSelectMod)
                // Capture strip
                CaptureStrip(capturing = state.isCapturing, onStart = onStartCapture, onStop = onStopCapture, clientUrl = state.clientUrl, selectedMod = selectedMod)
                // Tabs
                TabRow(selectedTabIndex = tab, containerColor = CardBlack, contentColor = NeonGreen, indicator = { tp -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tp[tab]), color = NeonGreen) }) {
                    listOf("CAPTURE", "LOGS").forEachIndexed { i, t ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(t, fontSize = 12.sp, fontFamily = FontFamily.Monospace); if (i == 0 && state.filteredRequests.isNotEmpty()) Badge(containerColor = NeonGreen.copy(0.1f)) { Text("${state.filteredRequests.size}", color = NeonGreen, fontSize = 9.sp) }; if (i == 1 && state.logs.isNotEmpty()) Badge(containerColor = ElectricBlue.copy(0.1f)) { Text("${state.logs.size}", color = ElectricBlue, fontSize = 9.sp) } } })
                    }
                }
            }
        },
        containerColor = DeepBlack
    ) { pv ->
        Box(Modifier.fillMaxSize().padding(pv)) {
            when (tab) {
                0 -> CaptureList(state.filteredRequests, state.responses, onNavigateToDetail, { req -> clipboard.setText(AnnotatedString(ExportUtils.buildRequestText(req))); snack = "Copied!" }, { req -> state.responses[req.id]?.let { clipboard.setText(AnnotatedString(ExportUtils.buildResponseText(req, it))); snack = "Response copied!" } }, { decodeTarget = it }, { req -> onSaveMod(req.endpoint, req.bodyText ?: ""); snack = "Mod saved!" })
                1 -> LogViewer(logs = state.logs, onClear = onClearLogs)
            }
        }
    }

    decodeTarget?.let { req -> FloatingDecodeOverlay(request = req, response = state.responses[req.id], onDismiss = { decodeTarget = null }, onSaveMod = { body -> onSaveMod(req.endpoint, body); decodeTarget = null }) }
    snack?.let { msg -> LaunchedEffect(msg) { kotlinx.coroutines.delay(1500); snack = null } }
}

@Composable
private fun ModSlider(mods: List<ModFile>, selectedMod: ModFile?, onSelectMod: (ModFile?) -> Unit) {
    Column(Modifier.fillMaxWidth().background(ElevatedBlack).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.Build, null, tint = PurpleAccent, modifier = Modifier.size(13.dp)); Text("SELECT MOD", color = PurpleAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
            if (selectedMod != null) TextButton(onClick = { onSelectMod(null) }, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("CLEAR", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(mods) { mod ->
                val isSel = selectedMod?.name == mod.name
                val tc = when (mod.type) { "REQUEST" -> NeonGreen; "HEADER" -> ElectricBlue; else -> PurpleAccent }
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (isSel) tc.copy(0.15f) else CardBlack).border(1.5.dp, if (isSel) tc else DividerGray, RoundedCornerShape(10.dp)).clickable { onSelectMod(if (isSel) null else mod) }.padding(horizontal = 14.dp, vertical = 8.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(if (isSel) Icons.Default.CheckCircle else Icons.Default.Build, null, tint = if (isSel) tc else TextSecondary, modifier = Modifier.size(16.dp))
                        Text(mod.name, color = if (isSel) tc else TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 80.dp))
                        Text(mod.type, color = tc.copy(0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureStrip(capturing: Boolean, onStart: () -> Unit, onStop: () -> Unit, clientUrl: String, selectedMod: ModFile?) {
    Row(Modifier.fillMaxWidth().background(CardBlack).padding(horizontal = 12.dp, vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (capturing) "● CAPTURING" else "○ IDLE", color = if (capturing) NeonGreen else TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(clientUrl.take(38), color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            if (selectedMod != null && !capturing) Text("MOD: ${selectedMod.name}", color = PurpleAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
        }
        if (capturing) OutlinedButton(onClick = onStop, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed), border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(0.5f))) { Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        else Button(onClick = onStart, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)) { Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(if (selectedMod != null) "Start +Mod" else "Start", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun CaptureList(requests: List<CapturedRequest>, responses: Map<String, CapturedResponse>, onTap: (CapturedRequest) -> Unit, onCopyReq: (CapturedRequest) -> Unit, onCopyRes: (CapturedRequest) -> Unit, onDecode: (CapturedRequest) -> Unit, onSaveMod: (CapturedRequest) -> Unit) {
    if (requests.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.SignalWifiOff, null, tint = TextDim, modifier = Modifier.size(48.dp)); Text("No requests captured", color = TextDim, fontSize = 14.sp); Text("Start capture to intercept traffic", color = TextDim.copy(0.6f), fontSize = 12.sp) } }
    else LazyColumn(state = rememberLazyListState(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(requests, key = { it.id }) { req -> UnifiedCaptureItem(request = req, response = responses[req.id], onTap = { onTap(req) }, onCopyRequest = { onCopyReq(req) }, onCopyResponse = { onCopyRes(req) }, onOpenDecode = { onDecode(req) }, onSaveMod = { onSaveMod(req) }) }
    }
}
