package com.example.workmanagerdemo.workers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.Data
import com.example.workmanagerdemo.R
import com.example.workmanagerdemo.contants.KEY_IMAGE_URI
import com.example.workmanagerdemo.contants.TAG_OUTPUT

class DailyAutoBlurWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val appContext = applicationContext
        
        makeStatusNotification("Starting daily automatic blur", appContext)
        
        return try {
            val imageUri = getImageUri(appContext)
            
            var continuation = WorkManager.getInstance(appContext)
                .beginWith(OneTimeWorkRequest.from(CleanupWorker::class.java))
            
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()
                .setInputData(createInputDataForUri(imageUri))
            
            continuation = continuation.then(blurBuilder.build())
            
            val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
                .addTag(TAG_OUTPUT)
                .build()
                
            continuation = continuation.then(save)
            
            continuation.enqueue()
            
            makeStatusNotification("Daily automatic blur completed", appContext)
            Result.success()
        } catch (throwable: Throwable) {
            makeStatusNotification("Daily automatic blur failed", appContext)
            Result.failure()
        }
    }
    
    private fun getImageUri(context: Context): Uri {
        val resources = context.resources
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()
    }
    
    private fun createInputDataForUri(imageUri: Uri): Data {
        return Data.Builder()
            .putString(KEY_IMAGE_URI, imageUri.toString())
            .build()
    }
}
