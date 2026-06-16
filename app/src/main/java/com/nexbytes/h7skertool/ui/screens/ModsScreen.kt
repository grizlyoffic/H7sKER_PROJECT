package com.nexbytes.h7skertool.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexbytes.h7skertool.ui.theme.*
import com.nexbytes.h7skertool.utils.ModFile
import com.nexbytes.h7skertool.utils.ModType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModsScreen(mods: List<ModFile>, onBack: () -> Unit, onDeleteMod: (String) -> Unit, onApplyMod: (ModFile) -> Unit) {
    val clipboard = LocalClipboardManager.current
    var expandedIdx by remember { mutableStateOf<Int?>(null) }
    var deleteConfirm by remember { mutableStateOf<String?>(null) }
    var snack by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Column { Text("MODZ", color = PurpleAccent, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp); Text("${mods.size} mods", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace) } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBlack)) },
        containerColor = DeepBlack
    ) { pv ->
        if (mods.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pv), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.FolderOff, null, tint = TextDim, modifier = Modifier.size(56.dp))
                    Text("No mods saved yet", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text("Decode an endpoint and tap Save Mod", color = TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(pv), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(mods.indices.toList()) { idx ->
                val mod = mods[idx]; val isExp = expandedIdx == idx
                val tc = when (mod.type) { ModType.REQUEST.name -> NeonGreen; ModType.HEADER.name -> ElectricBlue; else -> PurpleAccent }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBlack).border(1.dp, if (isExp) tc.copy(0.4f) else DividerGray, RoundedCornerShape(12.dp))) {
                    Row(Modifier.fillMaxWidth().clickable { expandedIdx = if (isExp) null else idx }.padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(tc.copy(0.12f)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text(mod.type, color = tc, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                            Column { Text(mod.name, color = TextBright, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text(SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(mod.createdAt)), color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                        }
                        Row {
                            IconButton(onClick = { onApplyMod(mod); snack = "Mod '${mod.name}' applied!" }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.PlayArrow, null, tint = NeonGreen, modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = { expandedIdx = if (isExp) null else idx }, modifier = Modifier.size(36.dp)) { Icon(if (isExp) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                        }
                    }
                    AnimatedVisibility(visible = isExp) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = DividerGray, thickness = 0.5.dp)
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ElevatedBlack).border(1.dp, tc.copy(0.2f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                                Text(mod.content.take(500) + if (mod.content.length > 500) "\n..." else "", color = tc.copy(0.9f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, overflow = TextOverflow.Ellipsis)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(mod.content)); snack = "Copied!" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Copy", fontSize = 12.sp) }
                                OutlinedButton(onClick = { deleteConfirm = mod.name }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Delete", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    deleteConfirm?.let { name ->
        AlertDialog(onDismissRequest = { deleteConfirm = null }, containerColor = ElevatedBlack, title = { Text("Delete mod?", color = AlertRed, fontWeight = FontWeight.Bold) }, text = { Text("Delete '$name'? Cannot be undone.", color = TextSecondary, fontSize = 13.sp) }, confirmButton = { TextButton(onClick = { onDeleteMod(name); deleteConfirm = null; snack = "Deleted '$name'" }) { Text("DELETE", color = AlertRed, fontWeight = FontWeight.ExtraBold) } }, dismissButton = { TextButton(onClick = { deleteConfirm = null }) { Text("Cancel", color = TextSecondary) } })
    }
    snack?.let { msg -> LaunchedEffect(msg) { kotlinx.coroutines.delay(2000); snack = null }; Box(Modifier.fillMaxSize().padding(bottom = 24.dp), Alignment.BottomCenter) { Snackbar(containerColor = ElevatedBlack) { Text(msg, color = NeonGreen, fontSize = 12.sp) } } }
}
