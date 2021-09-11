/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.*
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToFileWorker


class BlurViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager: WorkManager = WorkManager.getInstance(application)

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null
    internal val outputWorkInfos: LiveData<List<WorkInfo>>
    internal val progressWorkInfos: LiveData<List<WorkInfo>>

    init {
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
        progressWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_PROGRESS)
    }

    private fun createInputDataForUri(): Data {
        val dataBuilder = Data.Builder()
        imageUri?.let {
            dataBuilder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return dataBuilder.build()
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java))

        // Add WorkRequests to blur the image the number of times requested
        for (i in 0 until blurLevel) {
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }
            blurBuilder.addTag(TAG_PROGRESS)
            continuation = continuation.then(blurBuilder.build())
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val saveFile = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .setConstraints(constraints)
            .addTag(TAG_OUTPUT)
            .build()

        continuation = continuation.then(saveFile)

        continuation.enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

    /**
     * Setters
     */
    internal fun setImageUri(uri: String?) {
        imageUri = uriOrNull(uri)
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }
}
