package com.developer_rahul.meetmind_ai.core.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.min

object ImageUtils {

    /**
     * Decodes a Base64 string (with or without data URI prefix) into an Compose ImageBitmap.
     */
    fun decodeBase64ToBitmap(base64Str: String?): ImageBitmap? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val pureBase64 = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }
            val decodedBytes = Base64.decode(pureBase64.trim(), Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reads an image Uri, rescales it defensively, and encodes it into a JPEG Base64 data URI string.
     */
    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 512,
        quality: Int = 80
    ): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                val width = originalBitmap.width
                val height = originalBitmap.height
                val ratio = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)

                val scaledBitmap = if (ratio < 1.0f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (width * ratio).toInt().coerceAtLeast(1),
                        (height * ratio).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    originalBitmap
                }

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val bytes = outputStream.toByteArray()
                "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }
}
