package com.nexbytes.h7skertool.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexbytes.h7skertool.model.CapturedRequest
import com.nexbytes.h7skertool.model.CapturedResponse
import com.nexbytes.h7skertool.ui.components.FloatingDecodeOverlay
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
fun RequestDetailScreen(request: CapturedRequest, response: CapturedResponse?, onBack: () -> Unit, onSaveMod: (String, String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var mainTab by remember { mutableIntStateOf(0) }
    var subTab by remember { mutableIntStateOf(0) }
    var showDecode by remember { mutableStateOf(false) }
    var snack by remember { mutableStateOf<String?>(null) }
    var showSplitScreen by remember { mutableStateOf(false) }
    var decodedSplit by remember { mutableStateOf<String?>(null) }
    var isDecoding by remember { mutableStateOf(false) }
    var showModDialog by remember { mutableStateOf(false) }
    var modContent by remember { mutableStateOf("") }
    val http = remember { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build() }

    fun getCurrentContent() = if (mainTab == 0) when (subTab) { 1 -> request.headersAsString(); 2 -> request.body?.let { HexUtils.toHexDump(it) } ?: ""; else -> request.bodyText ?: "" } else when (subTab) { 1 -> response?.headersAsString() ?: ""; 2 -> response?.body?.let { HexUtils.toHexDump(it) } ?: ""; else -> response?.bodyText ?: "" }

    fun decodeAndSplit(ti: Int) {
        val hex = if (ti == 0) request.body?.let { HexUtils.toCleanHex(it) } else response?.body?.let { HexUtils.toCleanHex(it) }
        if (hex.isNullOrEmpty()) { snack = "No hex data"; return }
        isDecoding = true
        scope.launch {
            try {
                val url = if (ti == 0) "http://node.mrkalpha.tech:19140/request" else "http://node.mrkalpha.tech:19140/response"
                val body = JSONObject().apply { put("hex", hex) }.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val resp = withContext(Dispatchers.IO) { http.newCall(Request.Builder().url(url).post(body).build()).execute() }
                val txt = resp.body?.string() ?: ""
                withContext(Dispatchers.Main) { isDecoding = false; decodedSplit = DecodeUtils.prettyPrintJson(txt); showSplitScreen = true }
            } catch (e: Exception) { withContext(Dispatchers.Main) { isDecoding = false; snack = "Decode failed: ${e.message}" } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text(request.endpoint, color = TextBright, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(request.method, color = when (request.method) { "GET" -> MethodGET; "POST" -> MethodPOST; "PUT" -> MethodPUT; "DELETE" -> MethodDELETE; else -> TextSecondary }, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold); response?.let { r -> val c = when { r.statusCode in 200..299 -> NeonGreen; r.statusCode in 400..499 -> Amber; else -> AlertRed }; Text("${r.statusCode}", color = c, fontSize = 10.sp, fontFamily = FontFamily.Monospace); Text("${r.durationMs}ms", color = TextDim, fontSize = 10.sp) } } } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } },
                actions = {
                    // VS/developer decode icon
                    IconButton(onClick = { showDecode = true }) { Icon(Icons.Default.Code, null, tint = Amber, modifier = Modifier.size(22.dp)) }
                    // MOD button
                    IconButton(onClick = { modContent = getCurrentContent(); showModDialog = true }) {
                        Box(Modifier.clip(RoundedCornerShape(5.dp)).background(PurpleAccent.copy(0.12f)).border(1.dp, PurpleAccent.copy(0.3f), RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text("MOD", color = PurpleAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                    }
                    // Split screen decode
                    IconButton(onClick = { decodeAndSplit(mainTab) }) { if (isDecoding) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ElectricBlue) else Icon(Icons.Default.VerticalSplit, null, tint = ElectricBlue, modifier = Modifier.size(22.dp)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBlack)
            )
        },
        containerColor = DeepBlack
    ) { pv ->
        Column(Modifier.fillMaxSize().padding(pv)) {
            if (showSplitScreen && decodedSplit != null) {
                // TOP: decoded view
                Column(Modifier.weight(1f).fillMaxWidth().background(DeepBlack)) {
                    Row(Modifier.fillMaxWidth().background(ElevatedBlack).padding(horizontal = 12.dp, vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Default.Code, null, tint = PurpleAccent, modifier = Modifier.size(14.dp)); Text("DECODED PATTERN", color = PurpleAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                        Row { IconButton(onClick = { clipboard.setText(AnnotatedString(decodedSplit ?: "")); snack = "Copied!" }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ContentCopy, null, tint = PurpleAccent, modifier = Modifier.size(14.dp)) }; IconButton(onClick = { showSplitScreen = false; decodedSplit = null }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(14.dp)) } }
                    }
                    HorizontalDivider(color = PurpleAccent.copy(0.2f), thickness = 1.dp)
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) { Text(decodedSplit ?: "", color = PurpleAccent.copy(0.9f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 17.sp) }
                }
                HorizontalDivider(color = PurpleAccent, thickness = 2.dp)
                // BOTTOM: normal view
                Column(Modifier.weight(1f).fillMaxWidth()) { NormalDetailView(request, response, mainTab, subTab, { mainTab = it; subTab = 0 }, { subTab = it }, onSaveMod, clipboard, { snack = it }) }
            } else {
                NormalDetailView(request, response, mainTab, subTab, { mainTab = it; subTab = 0 }, { subTab = it }, onSaveMod, clipboard, { snack = it })
            }
        }
    }

    if (showDecode) FloatingDecodeOverlay(request = request, response = response, onDismiss = { showDecode = false }, onSaveMod = { body -> onSaveMod(request.endpoint, body) })

    if (showModDialog) AlertDialog(onDismissRequest = { showModDialog = false }, containerColor = ElevatedBlack, shape = RoundedCornerShape(16.dp), title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Build, null, tint = PurpleAccent, modifier = Modifier.size(18.dp)); Text("Create Mod", color = PurpleAccent, fontWeight = FontWeight.Bold) } }, text = { OutlinedTextField(value = modContent, onValueChange = { modContent = it }, label = { Text("Mod content", color = TextSecondary, fontSize = 12.sp) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurpleAccent, unfocusedBorderColor = DividerGray, focusedTextColor = TextBright, unfocusedTextColor = TextPrimary, cursorColor = PurpleAccent), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), minLines = 4, maxLines = 8) }, confirmButton = { TextButton(onClick = { onSaveMod(request.endpoint, modContent); showModDialog = false; snack = "Mod saved!" }) { Text("SAVE", color = PurpleAccent, fontWeight = FontWeight.ExtraBold) } }, dismissButton = { TextButton(onClick = { showModDialog = false }) { Text("Cancel", color = TextSecondary) } })

    snack?.let { msg -> LaunchedEffect(msg) { kotlinx.coroutines.delay(1800); snack = null }; Box(Modifier.fillMaxSize().padding(bottom = 16.dp), Alignment.BottomCenter) { Snackbar(containerColor = ElevatedBlack) { Text(msg, color = NeonGreen, fontSize = 12.sp) } } }
}

@Composable
private fun NormalDetailView(request: CapturedRequest, response: CapturedResponse?, mainTab: Int, subTab: Int, onMainTab: (Int) -> Unit, onSubTab: (Int) -> Unit, onSaveMod: (String, String) -> Unit, clipboard: androidx.compose.ui.platform.ClipboardManager, onSnack: (String) -> Unit) {
    val isReq = mainTab == 0
    TabRow(selectedTabIndex = mainTab, containerColor = CardBlack, contentColor = NeonGreen, indicator = { tp -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tp[mainTab]), color = NeonGreen) }) {
        listOf("REQUEST", "RESPONSE").forEachIndexed { i, t ->
            Tab(selected = mainTab == i, onClick = { onMainTab(i) }, text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text(t, fontSize = 11.sp, fontFamily = FontFamily.Monospace); if (i == 1 && response != null) { val c = when { response.statusCode in 200..299 -> NeonGreen; response.statusCode in 400..499 -> Amber; else -> AlertRed }; Badge(containerColor = c.copy(0.15f)) { Text("${response.statusCode}", color = c, fontSize = 9.sp) } } } })
        }
    }
    ScrollableTabRow(selectedTabIndex = subTab, containerColor = ElevatedBlack, contentColor = ElectricBlue, edgePadding = 0.dp, indicator = { tp -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tp[subTab]), color = ElectricBlue, height = 1.5.dp) }, divider = {}) {
        listOf("BODY", "HEADERS", "HEX").forEachIndexed { i, t -> Tab(selected = subTab == i, onClick = { onSubTab(i) }, text = { Text(t, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }) }
    }
    HorizontalDivider(color = DividerGray, thickness = 0.5.dp)
    Row(Modifier.fillMaxWidth().background(ElevatedBlack).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { val txt = if (isReq) when (subTab) { 1 -> request.headersAsString(); 2 -> request.body?.let { HexUtils.toHexDump(it) } ?: ""; else -> request.bodyText ?: "" } else when (subTab) { 1 -> response?.headersAsString() ?: ""; 2 -> response?.body?.let { HexUtils.toHexDump(it) } ?: ""; else -> response?.bodyText ?: "" }; clipboard.setText(AnnotatedString(txt)); onSnack("Copied!") }, colors = ButtonDefaults.textButtonColors(contentColor = NeonGreen)) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)); Text("Copy", fontSize = 11.sp) }
        if (isReq && request.bodyText?.isNotEmpty() == true && subTab == 0) TextButton(onClick = { onSaveMod(request.endpoint, request.bodyText ?: ""); onSnack("Saved as mod!") }, colors = ButtonDefaults.textButtonColors(contentColor = PurpleAccent)) { Icon(Icons.Default.Save, null, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)); Text("Save Mod", fontSize = 11.sp) }
    }
    HorizontalDivider(color = DividerGray, thickness = 0.5.dp)
    val content = if (isReq) when (subTab) { 1 -> request.headersAsString(); 2 -> request.body?.let { HexUtils.toHexDump(it) } ?: "(no hex)"; else -> request.bodyText ?: "(empty)" } else when (subTab) { 1 -> response?.headersAsString() ?: "(no response)"; 2 -> response?.body?.let { HexUtils.toHexDump(it) } ?: "(no hex)"; else -> response?.bodyText ?: "(waiting...)" }
    val textColor = when { subTab == 2 -> Amber.copy(0.85f); subTab == 1 -> TextSecondary; isReq -> NeonGreen.copy(0.9f); else -> ElectricBlue.copy(0.9f) }
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) { Text(content, color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 17.sp, softWrap = true) }
}
