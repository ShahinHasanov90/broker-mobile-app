package io.shahinhasanov.broker.data.outbox

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.shahinhasanov.broker.data.repository.DeclarationRepository

@HiltWorker
class OutboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DeclarationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = runCatching { repository.drainOutbox() }
        return result.fold(
            onSuccess = { drain ->
                if (drain.failed == 0) Result.success() else Result.retry()
            },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        const val UNIQUE_NAME = "broker-outbox"

        fun enqueue(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<OutboxWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
