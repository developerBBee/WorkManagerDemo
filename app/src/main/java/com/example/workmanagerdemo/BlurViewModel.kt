package com.example.workmanagerdemo

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.workmanagerdemo.contants.IMAGE_MANIPULATION_WORK_NAME
import com.example.workmanagerdemo.contants.KEY_IMAGE_URI
import com.example.workmanagerdemo.contants.TAG_OUTPUT
import com.example.workmanagerdemo.workers.BlurWorker
import com.example.workmanagerdemo.workers.CleanupWorker
import com.example.workmanagerdemo.workers.SaveImageToFileWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BlurViewModel(application: Application) : ViewModel() {

    private val workManager = WorkManager.getInstance(application)
    private val _workStateFlow = MutableStateFlow(UiState())
    val workStateFlow: StateFlow<UiState> = _workStateFlow.asStateFlow()

    private val imageUri: Uri by lazy { getImageUri(application.applicationContext) }
    internal var outputUri: Uri? = null

    init {
        workManager.getWorkInfosByTagFlow(TAG_OUTPUT)
            .onEach {
                if (it.isEmpty()) {
                    return@onEach
                }

                val workInfo = it.first()
                if (workInfo.state.isFinished) {
                    val outputUri = workInfo.outputData.getString(KEY_IMAGE_URI)
                    setOutputUri(outputUri)

                    _workStateFlow.value = UiState(
                        workingState = WorkingState.FINISHED,
                        hasOutputUri = !outputUri.isNullOrEmpty()
                    )
                } else {
                    _workStateFlow.value = UiState(workingState = WorkingState.IN_PROGRESS)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        // Add WorkRequest to Cleanup temporary images
        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )

        // Add WorkRequests to blur the image the number of times requested
        for (i in 0 until blurLevel) {
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }

            continuation = continuation.then(blurBuilder.build())
        }

        // Put this inside the applyBlur() function, above the save work request.
        // Create charging constraint
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        // Add WorkRequest to save the image to the filesystem
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .setConstraints(constraints)
            .addTag(TAG_OUTPUT)
            .build()

        continuation = continuation.then(save)

        // Actually start the work
        continuation.enqueue()
    }

    /**
     * Creates the input data bundle which includes the Uri to operate on
     * @return Data which contains the Image Uri as a String
     */
    private fun createInputDataForUri(): Data {
        return Data.Builder()
            .putString(KEY_IMAGE_URI, imageUri.toString())
            .build()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        val imageUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        return imageUri
    }

    private fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

    class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

data class UiState(
    val workingState: WorkingState = WorkingState.INITIAL_STATE,
    val hasOutputUri: Boolean = false,
)

enum class WorkingState {
    INITIAL_STATE,
    IN_PROGRESS,
    FINISHED,
}