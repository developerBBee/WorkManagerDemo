package com.example.workmanagerdemo.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.contants.KEY_IMAGE_URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveImageToFileWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd HH:mm:ss",
        Locale.getDefault()
    )

    override fun doWork(): Result {
        // Makes a notification when the work starts and slows down the work so that
        // it's easier to see each WorkRequest start, even on emulated devices
        makeStatusNotification("Saving image", applicationContext)
        sleep()

        val resolver = applicationContext.contentResolver
        return try {
            val resourceUri = inputData.getString(KEY_IMAGE_URI)
            Log.d(TAG, "Start saving. resourceUri=$resourceUri")

            val bitmap = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(resourceUri))
            )
            val contentValues = contentValuesOf(
                MediaStore.Images.Media.DISPLAY_NAME to "${title}_${dateFormatter.format(Date())}",
                MediaStore.Images.Media.MIME_TYPE to "image/png",
            )

            val imageUri = resolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            } ?: throw IllegalStateException("Writing to MediaStore failed")

            Log.d(TAG, "Success saving. imageUri=$imageUri")
            val outputData = workDataOf(KEY_IMAGE_URI to imageUri.toString())
            Result.success(outputData)
        } catch (throwable: Throwable) {
            Log.e(TAG, "Error saving image", throwable)
            Result.failure()
        }
    }

    companion object {
        private val TAG = SaveImageToFileWorker::class.java.simpleName
    }
}