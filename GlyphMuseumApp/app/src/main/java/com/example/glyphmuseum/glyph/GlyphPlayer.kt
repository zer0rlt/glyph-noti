package com.example.glyphmuseum.glyph

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream

class GlyphPlayer(private val context: Context) {
    
    private val matrixManager = GlyphMatrixManager.getInstance(context)
    
    private var isPlaying = false
    private var playJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isInitialized = false

    private val glyphCallback = object : com.nothing.ketchum.GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
            isInitialized = true
            // we register here to make sure it's bound before playing
            matrixManager?.register("Glyph.DEVICE_23112") 
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            isInitialized = false
        }
    }

    init {
        matrixManager?.init(glyphCallback)
    }

    fun playGif(file: File) {
        if (!isInitialized || matrixManager == null) return
        stop()

        playJob = coroutineScope.launch {
            isPlaying = true
            try {
                // If it's a direct app usage, we use setAppMatrixFrame, and registration is probably "Glyph.DEVICE_23112".
                // In demo, it uses com.nothing.ketchum.Glyph.DEVICE_23112
                matrixManager.register("Glyph.DEVICE_23112")
                
                val decoder = GifDecoder()
                val frames = FileInputStream(file).use {
                    decoder.decodeFrames(it, 25)
                }

                if (frames.isEmpty()) return@launch

                repeat(3) {
                    for (frame in frames) {
                        if (!isPlaying) break
                        try {
                            matrixManager.setAppMatrixFrame(frame.pixels)
                            delay(frame.delayMs.toLong())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            break
                        }
                    }
                }
            } finally {
                try {
                    matrixManager.closeAppMatrix()
                } catch (e: Exception) {}
                isPlaying = false
            }
        }
    }

    fun stop() {
        isPlaying = false
        playJob?.cancel()
        if (isInitialized) {
             try {
                 matrixManager?.closeAppMatrix()
             } catch (e: Exception) {}
        }
    }

    fun destroy() {
        stop()
        coroutineScope.cancel()
        try {
            matrixManager?.unInit()
        } catch (e: Exception) {}
    }
}
