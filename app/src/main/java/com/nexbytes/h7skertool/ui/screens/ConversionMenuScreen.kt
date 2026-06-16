package com.nexbytes.h7skertool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.nexbytes.h7skertool.ui.theme.*
import com.nexbytes.h7skertool.utils.ConversionUtils

data class ConversionItem(val label: String, val inputHint: String, val outputLabel: String, val color: Color, val convert: (String) -> String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionMenuScreen(onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var selectedIdx by remember { mutableIntStateOf(-1) }
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var snack by remember { mutableStateOf<String?>(null) }

    val conversions = listOf(
        ConversionItem("Hex → Base64", "hex bytes (e.g. 1a 72 5b)", "Base64", ElectricBlue) { ConversionUtils.hexToBase64(it) },
        ConversionItem("Base64 → Hex", "Base64 string", "Hex", NeonGreen) { ConversionUtils.base64ToHex(it) },
        ConversionItem("Hex → Binary", "hex bytes", "Binary", Amber) { ConversionUtils.hexToBinary(it) },
        ConversionItem("Binary → Hex", "binary (8 bits/byte)", "Hex", PurpleAccent) { ConversionUtils.binaryToHex(it) },
        ConversionItem("Hex → UTF-8", "hex bytes", "UTF-8 text", NeonGreen) { ConversionUtils.hexToUtf8(it) },
        ConversionItem("UTF-8 → Hex", "text string", "Hex", ElectricBlue) { ConversionUtils.utf8ToHex(it) },
        ConversionItem("Hex → Decimal", "hex (max 8 bytes)", "Decimal", Amber) { ConversionUtils.hexToDecimal(it) },
        ConversionItem("Decimal → Hex", "decimal number", "Hex", PurpleAccent) { ConversionUtils.decimalToHex(it) },
        ConversionItem("Hex → ASCII", "hex bytes", "ASCII chars", NeonGreen) { ConversionUtils.hexToAscii(it) },
        ConversionItem("ASCII → Hex", "ASCII text", "Hex", ElectricBlue) { ConversionUtils.asciiToHex(it) },
        ConversionItem("Base64 → Binary", "Base64 string", "Binary", Amber) { ConversionUtils.base64ToBinary(it) },
        ConversionItem("Binary → Base64", "binary string", "Base64", PurpleAccent) { ConversionUtils.binaryToBase64(it) },
        ConversionItem("Hex → Int32 LE", "4 hex bytes", "Int32 LE", NeonGreen) { ConversionUtils.hexToInt32LE(it) },
        ConversionItem("Hex → Int32 BE", "4 hex bytes", "Int32 BE", ElectricBlue) { ConversionUtils.hexToInt32BE(it) },
        ConversionItem("Hex → Octal", "hex bytes", "Octal", Amber) { ConversionUtils.hexToOctal(it) },
        ConversionItem("Reverse Hex", "hex bytes", "Reversed", PurpleAccent) { ConversionUtils.reverseHex(it) },
        ConversionItem("XOR Hex", "hex1|hex2 (pipe-sep)", "XOR", NeonGreen) { s -> val p = s.split("|", limit = 2); if (p.size == 2) ConversionUtils.xorHex(p[0].trim(), p[1].trim()) else null },
        ConversionItem("Varint → Decimal", "hex varint bytes", "Decimal", ElectricBlue) { ConversionUtils.hexToVarint(it) },
        ConversionItem("Decimal → Varint", "decimal number", "Varint hex", Amber) { ConversionUtils.varintToHex(it) },
        ConversionItem("Hex → Protobuf", "protobuf hex bytes", "Decoded fields", PurpleAccent) { ConversionUtils.hexToProto(it) },
        ConversionItem("URL → Hex", "URL-encoded string", "Hex", NeonGreen) { ConversionUtils.urlToHex(it) },
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("CONVERSION MENU", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBlack)) },
        containerColor = DeepBlack
    ) { pv ->
        LazyColumn(Modifier.fillMaxSize().padding(pv), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Select conversion type:", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace); Spacer(Modifier.height(4.dp)) }
            items(conversions.indices.toList()) { idx ->
                val conv = conversions[idx]; val isSel = selectedIdx == idx
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isSel) conv.color.copy(0.08f) else CardBlack).border(1.dp, if (isSel) conv.color.copy(0.5f) else DividerGray, RoundedCornerShape(12.dp))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(conv.color))
                            Text(conv.label, color = if (isSel) conv.color else TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                        }
                        IconButton(onClick = { selectedIdx = if (isSel) -1 else idx; input = ""; output = "" }, modifier = Modifier.size(32.dp)) {
                            Icon(if (isSel) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = if (isSel) conv.color else TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (isSel) Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = input, onValueChange = { input = it; output = "" }, modifier = Modifier.fillMaxWidth(), label = { Text("Input", color = TextSecondary, fontSize = 11.sp) }, placeholder = { Text(conv.inputHint, color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }, textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextBright), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = conv.color, unfocusedBorderColor = DividerGray, cursorColor = conv.color), shape = RoundedCornerShape(8.dp), minLines = 2, maxLines = 5)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { output = conv.convert(input.trim()) ?: "❌ Conversion failed" }, colors = ButtonDefaults.buttonColors(containerColor = conv.color), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Transform, null, tint = Color.Black, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("CONVERT", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (output.isNotEmpty()) IconButton(onClick = { clipboard.setText(AnnotatedString(output)); snack = "Copied!" }, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ElevatedBlack)) { Icon(Icons.Default.ContentCopy, null, tint = conv.color) }
                        }
                        if (output.isNotEmpty()) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ElevatedBlack).border(1.dp, conv.color.copy(0.3f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                            Text(conv.outputLabel, color = conv.color, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(output, color = TextBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    snack?.let { msg -> LaunchedEffect(msg) { kotlinx.coroutines.delay(1500); snack = null }; Box(Modifier.fillMaxSize().padding(bottom = 24.dp), Alignment.BottomCenter) { Snackbar(containerColor = ElevatedBlack) { Text(msg, color = NeonGreen, fontSize = 12.sp) } } }
}
