package com.example.glyphmuseum.ui

import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.glyphmuseum.data.AppDatabase
import com.example.glyphmuseum.data.GifEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(database: AppDatabase) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var gifs by remember { mutableStateOf(emptyList<GifEntity>()) }
    
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var newGifName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        database.gifDao().getAllGifs().collectLatest { gifs = it }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            showNameDialog = true
        }
    }

    if (showNameDialog && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { 
                showNameDialog = false
                pendingUri = null
            },
            title = { Text("Name your GIF") },
            text = {
                OutlinedTextField(
                    value = newGifName,
                    onValueChange = { newGifName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val uri = pendingUri!!
                    coroutineScope.launch {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val fileName = "gif_\${System.currentTimeMillis()}.gif"
                            val file = File(context.filesDir, fileName)
                            val outputStream = FileOutputStream(file)
                            inputStream.copyTo(outputStream)
                            inputStream.close()
                            outputStream.close()

                            database.gifDao().insertGif(
                                GifEntity(name = newGifName.ifBlank { "Imported GIF" }, filePath = file.absolutePath)
                            )
                        }
                    }
                    showNameDialog = false
                    pendingUri = null
                    newGifName = ""
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    pendingUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gallery", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch("image/gif") }) {
                Icon(Icons.Default.Add, contentDescription = "Import GIF")
            }
        }
    ) { padding ->
        if (gifs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No GIFs imported yet.")
            }
        } else {
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gifs) { gif ->
                    Card(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = File(gif.filePath),
                                    imageLoader = imageLoader
                                ),
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            Text(
                                text = gif.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
