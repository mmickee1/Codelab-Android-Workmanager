package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.PROGRESS
import com.example.background.R
import timber.log.Timber


class BlurWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        makeStatusNotification("Blurring image", appContext)
        val resourceUri = inputData.getString(KEY_IMAGE_URI)

        (0..100 step 10).forEach {
            setProgressAsync(workDataOf(PROGRESS to it))
            sleep()
        }

        return try {
            if (TextUtils.isEmpty(resourceUri)) {
                Timber.e("Invalid input uri")
                throw IllegalArgumentException("Invalid input uri")
            }

            val resolver = appContext.contentResolver
            val picture = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(resourceUri)))

            val blurredBitmap = blurBitmap(picture, appContext)

            val tempFileUri = writeBitmapToFile(appContext, blurredBitmap)

            makeStatusNotification("Output $tempFileUri", appContext)
            val outputData = workDataOf(KEY_IMAGE_URI to tempFileUri.toString())

            Result.success(outputData)
        } catch (throwable: Throwable) {
            Timber.e(throwable, "Error during blur")
            Result.failure()
        }



    }

}