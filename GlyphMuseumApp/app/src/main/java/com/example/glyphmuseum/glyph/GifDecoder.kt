package com.example.glyphmuseum.glyph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Movie
import java.io.InputStream
import kotlin.math.max

// Simple wrapper around android.graphics.Movie to extract frames
class GifDecoder {
    data class Frame(val pixels: IntArray, val delayMs: Int)

    fun decodeFrames(inputStream: InputStream, targetSize: Int): List<Frame> {
        val movie = Movie.decodeStream(inputStream) ?: return emptyList()
        val duration = movie.duration()
        val frames = mutableListOf<Frame>()
        
        if (duration == 0) {
            return emptyList()
        }

        // We will sample at ~30 fps or based on standard 33ms step
        val stepMs = 33
        var currentTime = 0
        
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = targetSize.toFloat() / max(movie.width(), movie.height()).toFloat()
        
        while (currentTime < duration) {
            movie.setTime(currentTime)
            canvas.drawColor(Color.BLACK)
            canvas.save()
            // Center the gif
            val dx = (targetSize - movie.width() * scale) / 2f
            val dy = (targetSize - movie.height() * scale) / 2f
            canvas.translate(dx, dy)
            canvas.scale(scale, scale)
            movie.draw(canvas, 0f, 0f)
            canvas.restore()

            val pixels = IntArray(targetSize * targetSize)
            bitmap.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)
            
            // Convert to grayscale 0-255 based on luminance (Glyph only supports intensity, not RGB)
            val grayscalePixels = IntArray(targetSize * targetSize)
            for (i in pixels.indices) {
                val color = pixels[i]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                grayscalePixels[i] = luma
            }
            
            frames.add(Frame(grayscalePixels, stepMs))
            currentTime += stepMs
        }
        
        return frames
    }
}
