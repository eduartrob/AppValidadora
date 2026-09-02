package mx.com.rutamovil.appvalidadora.data.sync

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.com.rutamovil.appvalidadora.common.util.NetworkTimeHelper
import mx.com.rutamovil.appvalidadora.data.local.AppDatabase
import mx.com.rutamovil.appvalidadora.data.local.entity.TransactionEntity
import mx.com.rutamovil.appvalidadora.data.local.entity.UltimaSincronizacionEntity
import mx.com.rutamovil.appvalidadora.data.remote.ApiService
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Trabajador en segundo plano encargado de la sincronización periódica de transacciones locales con el servidor.
 * Utiliza WorkManager para asegurar que los datos de cobro se envíen incluso si la aplicación está cerrada.
 * Implementa lógica de reintento ante fallos de conexión y control de frecuencia de sincronización.
 */
class SyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "SyncWorker"
    }

    /**
     * Ejecuta la tarea de sincronización.
     * Valida la existencia de transacciones pendientes y la disponibilidad de conexión a internet.
     *
     * @return Resultado del trabajo (Success, Failure o Retry).
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 ===== INICIANDO SYNCWORKER =====")

        val forceSync = inputData.getBoolean("force_sync", false)
        val db = AppDatabase.getInstance(applicationContext)

        // Verificación de existencia de datos pendientes para evitar consumo innecesario.
        val pendientes = db.transactionDao().countPending()
        if (pendientes == 0) return@withContext Result.success()

        // Verificación de conectividad.
        if (!hayInternet()) return@withContext Result.retry()

        // Procesamiento de sincronización forzada.
        if (forceSync) {
            val pendingTransactions = db.transactionDao().getPendingTransactions()
            return@withContext if (sincronizarTransacciones(db, pendingTransactions)) Result.success() else Result.retry()
        }

        // Lógica de intervalo para sincronización automática (cada 15 minutos).
        val ultimaSyncLocal = db.ultimaSincronizacionDao().getRegistroUnico()
        if (ultimaSyncLocal != null && ultimaSyncLocal.deviceUptimeAtSync > 0) {
            val uptimeActual = SystemClock.elapsedRealtime()
            val diferenciaMinutos = TimeUnit.MILLISECONDS.toMinutes(uptimeActual - ultimaSyncLocal.deviceUptimeAtSync)

            if (diferenciaMinutos >= 15) {
                val pendingTransactions = db.transactionDao().getPendingTransactions()
                return@withContext if (sincronizarTransacciones(db, pendingTransactions)) Result.success() else Result.retry()
            }
            return@withContext Result.success()
        } else {
            val pendingTransactions = db.transactionDao().getPendingTransactions()
            return@withContext if (sincronizarTransacciones(db, pendingTransactions)) Result.success() else Result.retry()
        }
    }

    /**
     * Orquesta el envío de transacciones a la API y actualiza los registros locales tras una confirmación exitosa.
     *
     * @param db Instancia de la base de datos local.
     * @param pendingTransactions Lista de entidades de transacciones a enviar.
     * @return Verdadero si todas las transacciones fueron sincronizadas correctamente.
     */
    private suspend fun sincronizarTransacciones(db: AppDatabase, pendingTransactions: List<TransactionEntity>): Boolean {
        val jsonArray = JsonArray()
        val idsToSync = mutableListOf<Int>()

        // Preparación del lote de transacciones en formato JSON.
        for (tx in pendingTransactions) {
            val jsonTx = JsonObject()
            jsonTx.addProperty("local_id", tx.id)
            jsonTx.addProperty("card_uuid", tx.cardUuid)
            jsonTx.addProperty("amount", tx.amount)
            jsonTx.addProperty("description", tx.description)
            jsonTx.addProperty("charged_at", tx.chargedAt)
            jsonTx.addProperty("device_uptime_ms", tx.deviceUptimeMs)
            jsonArray.add(jsonTx)
            idsToSync.add(tx.id)
        }

        val response = ApiService.syncTransactionsSync(jsonArray)

        // Procesamiento de la respuesta del servidor.
        if (response != null && response.has("status") && response.get("status").asBoolean) {
            val serverTime = if (response.has("server_time") && !response.get("server_time").isJsonNull) 
                response.get("server_time").asString 
                else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

            // Actualización del metadato de última sincronización.
            val registro = db.ultimaSincronizacionDao().getRegistroUnico() ?: UltimaSincronizacionEntity()
            registro.apply {
                fechaSincronizacion = serverTime
                deviceUptimeAtSync = SystemClock.elapsedRealtime()
                networkTimeAtSync = System.currentTimeMillis()
                tipo = "AUTOMATICA"
                transaccionesSincronizadas = pendingTransactions.size
                resultado = "EXITO"
            }

            if (registro.id != 0) db.ultimaSincronizacionDao().update(registro)
            else db.ultimaSincronizacionDao().insert(registro)

            // Marcado de transacciones individuales como sincronizadas.
            for (id in idsToSync) {
                db.transactionDao().updateServerTimes(id, serverTime)
            }

            // Notificación al sistema mediante broadcast para actualización de UI.
            applicationContext.sendBroadcast(Intent("SYNC_SUCCESS").apply {
                putExtra("server_time", serverTime)
            })

            return db.transactionDao().countPending() == 0
        }
        return false
    }

    /**
     * Consulta el estado de conectividad de red del dispositivo.
     *
     * @return Verdadero si hay una red activa conectada.
     */
    private fun hayInternet(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetwork != null
    }
}
