package com.iseeu.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Downscales a picked photo to a small square JPEG and Base64-encodes it, so it can be stored
 * directly on the member's Firestore document — Cloud Storage now requires the paid Blaze plan
 * even at zero usage, which this app's whole design deliberately avoids (see osmdroid-over-Google-Maps
 * for the same reasoning). At TARGET_SIZE/JPEG_QUALITY below, the encoded result is on the order of
 * tens of KB — far under Firestore's 1 MiB document limit.
 */
object AvatarImageProcessor {
    private const val TAG = "AvatarImageProcessor"
    private const val TARGET_SIZE = 256
    private const val JPEG_QUALITY = 70

    fun compressToBase64(context: Context, uri: Uri): String? {
        // Read the picked photo exactly once — some providers (cloud-backed gallery photos,
        // certain OEM pickers) don't reliably support opening the same content:// URI more than
        // once, which silently broke this when bounds/decode/EXIF each opened their own stream.
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "failed to read picked image", e)
            null
        } ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Log.e(TAG, "could not decode image bounds")
            return null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, TARGET_SIZE)
        }
        val sampled = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        if (sampled == null) {
            Log.e(TAG, "could not decode sampled bitmap")
            return null
        }

        // Best-effort — a photo that fails EXIF parsing should still upload, just possibly
        // not rotated correctly, rather than failing the whole upload.
        val rotationDegrees = runCatching { readExifRotationDegrees(bytes) }.getOrDefault(0)
        val rotated = if (rotationDegrees != 0) rotateBitmap(sampled, rotationDegrees) else sampled

        val squared = Bitmap.createScaledBitmap(centerCropSquare(rotated), TARGET_SIZE, TARGET_SIZE, true)

        val output = ByteArrayOutputStream()
        squared.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= target && h / 2 >= target) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }
        return sampleSize
    }

    private fun readExifRotationDegrees(bytes: ByteArray): Int {
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun centerCropSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }
}
