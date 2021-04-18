package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.background.R
import timber.log.Timber


class BlurWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        makeStatusNotification("Blurring image", appContext)

        return try {

            val picture = BitmapFactory.decodeResource(
                appContext.resources,
                R.drawable.test
            )

            val blurredBitmap = blurBitmap(picture, appContext)

            val tempFileUri = writeBitmapToFile(appContext, blurredBitmap)

            makeStatusNotification("Output $tempFileUri", appContext)
            Result.success()
        } catch (throwable: Throwable) {
            Timber.e(throwable, "Error during blur")
            Result.failure()
        }



    }

}