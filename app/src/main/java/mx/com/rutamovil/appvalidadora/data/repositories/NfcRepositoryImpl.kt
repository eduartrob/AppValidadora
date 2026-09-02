package mx.com.rutamovil.appvalidadora.data.repositories

import android.content.Context
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.com.rutamovil.appvalidadora.common.Constants
import mx.com.rutamovil.appvalidadora.common.util.Utils
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.DESFireAdapter
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.DESFireEV1
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.KeyType
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.command.DefaultIsoDepWrapper
import mx.com.rutamovil.appvalidadora.data.local.AppDatabase
import mx.com.rutamovil.appvalidadora.data.local.entity.TransactionEntity
import mx.com.rutamovil.appvalidadora.domain.repositories.INfcRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementación del repositorio de NFC encargada de la comunicación directa con el chip físico.
 * Utiliza el protocolo ISO-DEP y una capa de adaptación para tarjetas DESFire EV1.
 * Gestiona la autenticación criptográfica, lectura de saldos, perfiles y procesos de cobro.
 *
 * @property context Contexto de la aplicación necesario para la persistencia de transacciones locales.
 */
class NfcRepositoryImpl(
    private val context: Context
) : INfcRepository {

    /** Objeto que gestiona la comunicación ISO-DEP de bajo nivel. */
    private var isoDep: IsoDep? = null
    
    /** Motor de comandos específico para tarjetas DESFire EV1. */
    private var desfireEV1: DESFireEV1? = null

    companion object {
        private const val TAG = "NfcRepository"
    }

    /**
     * Establece una conexión activa con una etiqueta NFC detectada.
     * Configura el adaptador y el motor de comandos para interactuar con chips DESFire.
     *
     * @param tag Objeto de etiqueta NFC proporcionado por el sistema Android.
     * @return Verdadero si la conexión y configuración inicial fueron exitosas.
     */
    override fun connect(tag: Any): Boolean {
        return try {
            val nfcTag = tag as? Tag ?: return false
            isoDep = IsoDep.get(nfcTag) ?: return false

            isoDep?.connect()
            isoDep?.timeout = 8000

            if (isoDep?.isConnected != true) return false

            val wrapper = DefaultIsoDepWrapper(isoDep!!)
            val adapter = DESFireAdapter(wrapper, false)
            desfireEV1 = DESFireEV1(adapter)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error conectando: ${e.message}")
            disconnect()
            false
        }
    }

    /**
     * Cierra la conexión activa con la tarjeta NFC y libera los recursos asociados.
     */
    override fun disconnect() {
        try {
            isoDep?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error desconectando", e)
        } finally {
            isoDep = null
            desfireEV1 = null
        }
    }

    /**
     * Realiza una autenticación criptográfica AES contra una aplicación específica de la tarjeta.
     *
     * @param aid Identificador de aplicación (AID) en formato hexadecimal.
     * @param keyHex Llave de autenticación AES en formato hexadecimal.
     * @return Verdadero si la tarjeta validó correctamente la llave.
     */
    override suspend fun authenticate(aid: String, keyHex: String): Boolean = withContext(Dispatchers.IO) {
        val desfire = desfireEV1 ?: throw Exception("Tarjeta no conectada")
        val aidBytes = Utils.hexStringToByteArray(aid) ?: throw Exception("AID inválido")
        val keyBytes = Utils.hexStringToByteArray(keyHex) ?: throw Exception("Llave inválida")

        if (!desfire.selectApplication(aidBytes)) {
            Log.e(TAG, "No se pudo seleccionar AID: $aid")
            return@withContext false
        }

        val success = desfire.authenticate(keyBytes, 0.toByte(), KeyType.AES)
        if (success) {
            Log.d(TAG, "Autenticación exitosa")
        } else {
            Log.e(TAG, "Autenticación fallida para AID: $aid")
        }
        success
    }

    /**
     * Lee el saldo actual almacenado en el archivo de valores de la tarjeta.
     * El valor se convierte de centavos a una representación decimal.
     *
     * @return El saldo disponible como un valor numérico Double.
     */
    override suspend fun readBalance(): Double = withContext(Dispatchers.IO) {
        val desfire = desfireEV1 ?: throw Exception("Tarjeta no conectada")
        val centavos = desfire.getValue(Constants.FILE_VALUE) ?: throw Exception("Error al leer saldo (PICC 9D?)")
        centavos / 100.0
    }

    /**
     * Obtiene el código de rol de usuario persistido físicamente en la tarjeta.
     *
     * @return Identificador numérico del rol de usuario.
     */
    override suspend fun readRole(): Int = withContext(Dispatchers.IO) {
        val desfire = desfireEV1 ?: throw Exception("Tarjeta no conectada")
        val data = desfire.readData(Constants.FILE_ROLE, 0, 1)
        if (data == null || data.isEmpty()) {
            Constants.ROLE_REGULAR.toInt()
        } else {
            data[0].toInt() and 0xFF
        }
    }

    /**
     * Verifica si la tarjeta se encuentra marcada como bloqueada en su archivo de estado interno.
     *
     * @return Verdadero si la tarjeta tiene el estado de bloqueo activo.
     */
    override suspend fun isCardBlocked(): Boolean = withContext(Dispatchers.IO) {
        val desfire = desfireEV1 ?: throw Exception("Tarjeta no conectada")
        val statusBytes = desfire.readData(Constants.FILE_STATUS, 0, 1)
        statusBytes != null && statusBytes.isNotEmpty() && statusBytes[0] == Constants.STATUS_BLOCKED
    }

    /**
     * Escribe permanentemente el estado de bloqueo en la tarjeta física.
     *
     * @return Verdadero si la escritura fue confirmada por el chip.
     */
    override suspend fun blockCardPhysically(): Boolean = withContext(Dispatchers.IO) {
        val desfire = desfireEV1 ?: throw Exception("Tarjeta no conectada")
        val statusData = ByteArray(1)
        statusData[0] = Constants.STATUS_BLOCKED
        desfire.writeData(Constants.FILE_STATUS, 0, statusData)
    }

    /**
     * Realiza un débito de saldo en la tarjeta y registra la transacción localmente si es exitoso.
     * Utiliza un proceso atómico (Commit) para asegurar que la transacción se complete en el chip.
     *
     * @param monto Cantidad monetaria a descontar del saldo de la tarjeta.
     * @return Verdadero si el cobro fue procesado por la tarjeta y persistido localmente.
     */
    override suspend fun debitBalance(monto: Double): Boolean = withContext(Dispatchers.IO) {
        val desfire = desfireEV1 ?: throw Exception("Tarjeta no conectada")
        
        val centavosADebitar = (monto * 100).toInt()
        val success = desfire.debit(Constants.FILE_VALUE, centavosADebitar)
        if (success) {
            val commit = desfire.commitTransaction()
            if (commit) {
                // Registro local de la transacción para posterior sincronización.
                val db = AppDatabase.getInstance(context)
                val uidHex = Utils.bytesToHex((isoDep?.tag as? Tag)?.id ?: byteArrayOf())
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val tx = TransactionEntity(
                    cardUuid = uidHex,
                    amount = monto,
                    description = "Cobro validador (NFC)",
                    chargedAt = timestamp,
                    synced = false,
                    deviceUptimeMs = SystemClock.elapsedRealtime()
                )
                db.transactionDao().insert(tx)
                return@withContext true
            }
        }
        false
    }
}
