package com.example.glyphmuseum

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
        // Ideally handled with a lifecycle observer in compose, but this works for simple setup
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
    
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, database)
        }
        composable("gallery") {
            GalleryScreen(navController, database)
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
