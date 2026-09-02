package mx.com.rutamovil.appvalidadora.presentation

import android.app.Application
import androidx.work.*
import mx.com.rutamovil.appvalidadora.data.sync.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * Clase principal de la aplicación que extiende de [Application].
 * Se encarga de la inicialización de configuraciones globales y la programación 
 * de tareas periódicas en segundo plano.
 */
class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configuración de la sincronización automática de transacciones.
        setupPeriodicSync()
    }

    /**
     * Configura y encola una tarea periódica utilizando WorkManager para la sincronización 
     * de datos con el servidor central cada 15 minutos, requiriendo conexión a red.
     */
    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES // Intervalo mínimo permitido por el sistema Android.
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PeriodicSyncTransactions",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }
}
