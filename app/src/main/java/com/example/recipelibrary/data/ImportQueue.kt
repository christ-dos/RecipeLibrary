package com.example.recipelibrary.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.recipelibrary.MainActivity
import com.example.recipelibrary.parseRecipeJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Traite les liens en attente un par un.
 * - Recette extraite : READY (à valider dans l'app).
 * - PC injoignable ou trop lent : on s'arrête et on réessaiera plus tard.
 * - Autre erreur (pas de recette, Instagram bloqué...) : FAILED avec la raison.
 */
class ImportQueueProcessor(
    private val dao: PendingImportDao,
    private val serverUrl: () -> String?,
    private val runImport: suspend (url: String, server: String) -> Result<String>,
    private val isReadableRecipe: (String) -> Boolean
) {
    data class Report(val retryLater: Boolean, val readyCount: Int)

    suspend fun processAll(): Report {
        var ready = 0
        while (true) {
            val item = dao.nextWaiting() ?: return Report(retryLater = false, readyCount = ready)
            val server = serverUrl()
            if (server.isNullOrBlank()) {
                // Sans adresse de serveur, rien ne peut marcher : on attend que l'utilisatrice la saisisse.
                return Report(retryLater = true, readyCount = ready)
            }
            val result = runImport(item.url, server)
            val ex = result.exceptionOrNull()
            when {
                result.isSuccess -> {
                    val json = result.getOrThrow()
                    if (isReadableRecipe(json)) {
                        dao.update(item.copy(status = PendingImport.READY, recipeJson = json, error = null))
                        ready++
                    } else {
                        dao.update(item.copy(status = PendingImport.FAILED, error = "Impossible de lire la recette reçue."))
                    }
                }
                isConnectionError(ex) || ex is SocketTimeoutException ->
                    return Report(retryLater = true, readyCount = ready)
                else ->
                    dao.update(item.copy(status = PendingImport.FAILED, error = ex?.message ?: "Erreur inconnue"))
            }
        }
    }
}

class ImportQueueWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        LanNetwork.init(applicationContext)
        val prefs = applicationContext.getSharedPreferences(HermesPrefs.NAME, Context.MODE_PRIVATE)
        val dao = DatabaseProvider.getDatabase(applicationContext).pendingImportDao()
        val importer = HermesImporter(prefs)
        val report = ImportQueueProcessor(
            dao = dao,
            serverUrl = { resolveStoredServerUrl(prefs).ifBlank { null } },
            runImport = { url, server -> importer.run(url, server).result },
            isReadableRecipe = { parseRecipeJson(it) != null }
        ).processAll()

        if (report.readyCount > 0) ImportQueue.notifyReady(applicationContext, report.readyCount)
        return if (report.retryLater) Result.retry() else Result.success()
    }
}

object ImportQueue {
    private const val WORK_NAME = "import_queue"
    private const val CHANNEL_ID = "import_queue"
    private const val NOTIFICATION_ID = 4242
    /** Extra de l'intent de la notification : ouvre l'écran d'import par URL. */
    const val EXTRA_OPEN_QUEUE = "open_import_queue"

    private fun dao(context: Context) = DatabaseProvider.getDatabase(context).pendingImportDao()

    /** Ajoute un lien à la file (sans doublon) et lance le traitement. */
    suspend fun add(context: Context, url: String) {
        val clean = url.trim()
        val dao = dao(context)
        val existing = dao.findByUrl(clean)
        when {
            existing == null -> dao.insert(PendingImport(url = clean))
            existing.status == PendingImport.FAILED ->
                dao.update(existing.copy(status = PendingImport.WAITING, error = null))
        }
        kick(context)
    }

    suspend fun retry(context: Context, item: PendingImport) {
        dao(context).update(item.copy(status = PendingImport.WAITING, error = null))
        kick(context)
    }

    suspend fun remove(context: Context, id: Long) = dao(context).deleteById(id)

    /** Relance le traitement s'il reste des liens en attente (à l'ouverture de l'app par exemple). */
    suspend fun kickIfWaiting(context: Context) {
        if (dao(context).countWaiting() > 0) kick(context)
    }

    /**
     * Planifie le traitement dès que le téléphone est sur un Wi-Fi.
     * Si un traitement tourne déjà, on passe après lui ; sinon on remplace celui qui attend
     * (pour ne pas subir un long délai de nouvel essai quand on rentre à la maison).
     */
    suspend fun kick(context: Context) = withContext(Dispatchers.IO) {
        val wm = WorkManager.getInstance(context)
        val running = runCatching {
            wm.getWorkInfosForUniqueWork(WORK_NAME).get().any { it.state == WorkInfo.State.RUNNING }
        }.getOrDefault(false)
        val request = OneTimeWorkRequestBuilder<ImportQueueWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            // Nouvel essai 1 min, 2 min, 3 min... après un échec (le PC peut avoir été rallumé entre-temps)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniqueWork(
            WORK_NAME,
            if (running) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun readyMessage(count: Int): String =
        if (count == 1) "1 recette importée, à valider" else "$count recettes importées, à valider"

    fun notifyReady(context: Context, count: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Recettes importées", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_QUEUE, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(readyMessage(count))
            .setContentText("Ouvre l'app pour vérifier la catégorie et les enregistrer.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}
