package com.example.glyphmuseum.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.glyphmuseum.data.AppDatabase
import com.example.glyphmuseum.data.RuleEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, database: AppDatabase) {
    val coroutineScope = rememberCoroutineScope()
    var rules by remember { mutableStateOf(emptyList<RuleEntity>()) }

    LaunchedEffect(Unit) {
        database.ruleDao().getAllRules().collectLatest {
            rules = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glyph Museum", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("editor/-1") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        if (rules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No rules found. Add one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rules) { rule ->
                    RuleCard(rule = rule, onClick = { navController.navigate("editor/\${rule.id}") }, onToggle = { isEnabled ->
                        coroutineScope.launch {
                            database.ruleDao().updateRule(rule.copy(isEnabled = isEnabled))
                        }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCard(rule: RuleEntity, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Priority: \${rule.priority}", style = MaterialTheme.typography.labelSmall)
                Text(text = if (rule.appPackages.isNullOrEmpty()) "Any App" else "Specific Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!rule.senderName.isNullOrEmpty()) {
                    Text(text = "Sender: \${rule.senderName}", style = MaterialTheme.typography.bodyMedium)
                }
                if (!rule.messageContains.isNullOrEmpty()) {
                    Text(text = "Contains: '\${rule.messageContains}'", style = MaterialTheme.typography.bodySmall)
                }
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
