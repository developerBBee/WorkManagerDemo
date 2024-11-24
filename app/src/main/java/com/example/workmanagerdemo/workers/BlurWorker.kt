package com.example.workmanagerdemo.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.contants.KEY_IMAGE_URI

class BlurWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val appContext = applicationContext

        makeStatusNotification("Blurring image", appContext)
        sleep()

        return try {
            val resourceUri = inputData.getString(KEY_IMAGE_URI)
            Log.d(TAG, "Start blurring. resourceUri=$resourceUri")

            if (resourceUri.isNullOrEmpty()) {
                throw IllegalArgumentException("Invalid input uri")
            }

            val resolver = appContext.contentResolver

            val picture = BitmapFactory
                .decodeStream(resolver.openInputStream(Uri.parse(resourceUri)))

            val output = blurBitmap(picture, appContext)
            val outputUri = writeBitmapToFile(appContext, output)

            makeStatusNotification("Output is $outputUri", appContext)

            Log.d(TAG, "Success blurring. outputUri=$outputUri")
            val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())
            Result.success(outputData)
        } catch (throwable: Throwable) {
            Log.e(TAG, "Error applying blur", throwable)
            Result.failure()
        }
    }

    companion object {
        private val TAG = BlurWorker::class.java.simpleName
    }
}