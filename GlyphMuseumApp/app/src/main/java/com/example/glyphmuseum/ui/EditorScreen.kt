package com.example.glyphmuseum.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    
    var priority by remember { mutableStateOf("1") }
    var appPackage by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }
    var messageContains by remember { mutableStateOf("") }
    var selectedGifId by remember { mutableStateOf<Long?>(null) }
    
    var availableGifs by remember { mutableStateOf(emptyList<GifEntity>()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        availableGifs = database.gifDao().getAllGifs().first()
        if (ruleId != -1L) {
            val rules = database.ruleDao().getAllRules().first()
            val rule = rules.find { it.id == ruleId }
            if (rule != null) {
                priority = rule.priority.toString()
                appPackage = rule.appPackage ?: ""
                senderName = rule.senderName ?: ""
                messageContains = rule.messageContains ?: ""
                selectedGifId = rule.gifId
            }
        } else if (availableGifs.isNotEmpty()) {
            selectedGifId = availableGifs.first().id
        }
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
            
            OutlinedTextField(
                value = appPackage,
                onValueChange = { appPackage = it },
                label = { Text("App Package (e.g., com.whatsapp, leave empty for any)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = senderName,
                onValueChange = { senderName = it },
                label = { Text("Sender Name (e.g., Mom)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = messageContains,
                onValueChange = { messageContains = it },
                label = { Text("Message Contains") },
                modifier = Modifier.fillMaxWidth()
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedGifName = availableGifs.find { it.id == selectedGifId }?.name ?: "Select GIF"
                OutlinedTextField(
                    value = selectedGifName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select GIF") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableGifs.forEach { gif ->
                        DropdownMenuItem(
                            text = { Text(gif.name) },
                            onClick = {
                                selectedGifId = gif.id
                                expanded = false
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
                            appPackage = appPackage.ifBlank { null },
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
