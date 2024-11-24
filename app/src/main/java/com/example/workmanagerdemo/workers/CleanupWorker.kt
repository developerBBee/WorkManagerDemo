package com.example.workmanagerdemo.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.workmanagerdemo.contants.OUTPUT_PATH
import java.io.File

class CleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // Makes a notification when the work starts and slows down the work so that
        // it's easier to see each WorkRequest start, even on emulated devices
        makeStatusNotification("Cleaning up old temporary files", applicationContext)
        sleep()

        return try {
            Log.d(TAG, "Start cleaning.")

            val outputDirectory = File(applicationContext.filesDir, OUTPUT_PATH)
            if (outputDirectory.exists()) {
                val entries = outputDirectory.listFiles()
                if (entries != null) {
                    for (entry in entries) {
                        val name = entry.name
                        if (name.isNotEmpty() && name.endsWith(".png")) {
                            val deleted = entry.delete()
                            Log.i(TAG, "Deleted $name - $deleted")
                        }
                    }
                }
            }

            Log.d(TAG, "Success cleaning.")
            Result.success()
        } catch (throwable: Throwable) {
            Log.e(TAG, "Error cleaning up", throwable)
            Result.failure()
        }
    }

    companion object {
        private val TAG = CleanupWorker::class.java.simpleName
    }
}