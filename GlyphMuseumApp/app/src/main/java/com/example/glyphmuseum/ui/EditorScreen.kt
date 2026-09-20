package com.example.glyphmuseum.ui

import androidx.compose.ui.text.font.FontWeight
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.glyphmuseum.data.AppDatabase
import com.example.glyphmuseum.data.GifEntity
import com.example.glyphmuseum.data.RuleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(navController: NavController, database: AppDatabase, ruleId: Long) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    var priority by remember { mutableStateOf("1") }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var senderName by remember { mutableStateOf("") }
    var messageContains by remember { mutableStateOf("") }
    var selectedGifId by remember { mutableStateOf<Long?>(null) }
    
    var availableGifs by remember { mutableStateOf(emptyList<GifEntity>()) }
    var gifDropdownExpanded by remember { mutableStateOf(false) }
    var showAppDialog by remember { mutableStateOf(false) }

    // Contact picker launcher
    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    senderName = cursor.getString(nameIndex)
                }
                cursor.close()
            }
        }
    }

    LaunchedEffect(Unit) {
        availableGifs = database.gifDao().getAllGifs().first()
        if (ruleId != -1L) {
            val rules = database.ruleDao().getAllRules().first()
            val rule = rules.find { it.id == ruleId }
            if (rule != null) {
                priority = rule.priority.toString()
                selectedPackages = rule.appPackages?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
                senderName = rule.senderName ?: ""
                messageContains = rule.messageContains ?: ""
                selectedGifId = rule.gifId
            }
        } else if (availableGifs.isNotEmpty()) {
            selectedGifId = availableGifs.first().id
        }
    }

    if (showAppDialog) {
        val installedApps = remember {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
                .sortedBy { packageManager.getApplicationLabel(it).toString() }
        }

        AlertDialog(
            onDismissRequest = { showAppDialog = false },
            title = { Text("Select Applications") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(installedApps) { appInfo ->
                        val isSelected = selectedPackages.contains(appInfo.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedPackages -= appInfo.packageName
                                    else selectedPackages += appInfo.packageName
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(packageManager.getApplicationLabel(appInfo).toString())
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAppDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId == -1L) "New Rule" else "Edit Rule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = priority,
                onValueChange = { priority = it },
                label = { Text("Priority (Lower = Higher Priority)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { showAppDialog = true }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monitored Apps", style = MaterialTheme.typography.labelMedium)
                    if (selectedPackages.isEmpty()) {
                        Text("Any App")
                    } else {
                        Text("\${selectedPackages.size} apps selected", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Sender Name (e.g., Mom)") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { contactLauncher.launch(null) }) {
                    Icon(Icons.Default.Person, contentDescription = "Pick Contact")
                }
            }

            OutlinedTextField(
                value = messageContains,
                onValueChange = { messageContains = it },
                label = { Text("Message Contains") },
                modifier = Modifier.fillMaxWidth()
            )
            
            ExposedDropdownMenuBox(
                expanded = gifDropdownExpanded,
                onExpandedChange = { gifDropdownExpanded = !gifDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedGifName = availableGifs.find { it.id == selectedGifId }?.name ?: "Select GIF"
                OutlinedTextField(
                    value = selectedGifName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select GIF") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gifDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = gifDropdownExpanded,
                    onDismissRequest = { gifDropdownExpanded = false }
                ) {
                    availableGifs.forEach { gif ->
                        DropdownMenuItem(
                            text = { Text(gif.name) },
                            onClick = {
                                selectedGifId = gif.id
                                gifDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        val rule = RuleEntity(
                            id = if (ruleId == -1L) 0 else ruleId,
                            priority = priority.toIntOrNull() ?: 99,
                            appPackages = if (selectedPackages.isEmpty()) null else selectedPackages.joinToString(","),
                            senderName = senderName.ifBlank { null },
                            messageContains = messageContains.ifBlank { null },
                            gifId = selectedGifId ?: 0
                        )
                        if (ruleId == -1L) {
                            database.ruleDao().insertRule(rule)
                        } else {
                            database.ruleDao().updateRule(rule)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedGifId != null
            ) {
                Text("Save Rule")
            }
        }
    }
}
