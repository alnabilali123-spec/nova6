package com.novatube.app.download

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DownloadScheduler {

    fun enqueue(context: Context, downloadId: Long, networkType: NetworkType = NetworkType.CONNECTED) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresStorageNotLow(true)
            .build()
        val work = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putLong(DownloadWorker.KEY_DOWNLOAD_ID, downloadId).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(tagFor(downloadId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(downloadId),
            ExistingWorkPolicy.KEEP,
            work
        )
    }

    fun cancel(context: Context, downloadId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(downloadId))
    }

    fun observe(context: Context, downloadId: Long, observer: Observer<List<WorkInfo>>) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(uniqueName(downloadId))
            .observeForever(observer)
    }

    fun tagFor(id: Long) = "novatube_dl_$id"
    private fun uniqueName(id: Long) = "novatube_dl_$id"
}
