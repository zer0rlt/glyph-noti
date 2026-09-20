package com.example.glyphmuseum

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.glyphmuseum.data.AppDatabase
import com.example.glyphmuseum.ui.EditorScreen
import com.example.glyphmuseum.ui.GalleryScreen
import com.example.glyphmuseum.ui.MainScreen
import com.example.glyphmuseum.ui.theme.GlyphMuseumTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(this)

        setContent {
            GlyphMuseumTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasPermission by remember { mutableStateOf(checkNotificationPermission()) }

                    if (!hasPermission) {
                        PermissionScreen(onGrantClick = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        })
                    } else {
                        AppNavigation(database)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission when coming back from settings
    }

    private fun checkNotificationPermission(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packages.contains(packageName)
    }
}

@Composable
fun PermissionScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Notification Access Required", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("This app needs access to your notifications to trigger the Glyph Matrix based on your rules.")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onGrantClick) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun AppNavigation(database: AppDatabase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == "main" || currentRoute == "gallery") {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Rules") },
                        label = { Text("Rules") },
                        selected = currentRoute == "main",
                        onClick = {
                            navController.navigate("main") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Gallery") },
                        label = { Text("Gallery") },
                        selected = currentRoute == "gallery",
                        onClick = {
                            navController.navigate("gallery") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("main") {
                MainScreen(navController, database)
            }
            composable("gallery") {
                GalleryScreen(database)
            }
            composable(
                route = "editor/{ruleId}",
                arguments = listOf(navArgument("ruleId") { type = NavType.LongType })
            ) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getLong("ruleId") ?: -1L
                EditorScreen(navController, database, ruleId)
            }
        }
    }
}
