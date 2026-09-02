package mx.com.rutamovil.appvalidadora.domain.helpers

import android.util.Log
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.DESFireEV1
import mx.com.rutamovil.appvalidadora.data.local.AppDatabase

/**
 * Asistente especializado en la gestión del estado de bloqueo de tarjetas físicas.
 * Interactúa con el archivo de estado (0x05) del chip DESFire y con la lista negra de la base de datos local.
 *
 * @property desfire Instancia del motor de protocolo DESFire.
 * @property uidHex Identificador físico de la tarjeta.
 */
class TarjetaBloqueoHelper(
    private val desfire: DESFireEV1,
    private val uidHex: String
) {
    companion object {
        private const val TAG = "TarjetaBloqueoHelper"
        private const val FILE_STATUS: Byte = 0x05
        private const val STATUS_UNBLOCKED: Byte = 0x00
        private const val STATUS_BLOCKED: Byte = 0x01
    }

    /**
     * Consulta el chip físico para determinar si la tarjeta está marcada como bloqueada.
     *
     * @return Verdadero si el byte de estado es 0x01.
     */
    fun isCardBlocked(): Boolean {
        return try {
            val readData = desfire.readData(FILE_STATUS, 0, 1)
            if (readData != null && readData.isNotEmpty()) {
                val status = readData[0]
                val blocked = (status == STATUS_BLOCKED)
                Log.d(TAG, "📌 Tarjeta $uidHex - Status: ${if (blocked) "BLOQUEADA" else "NO BLOQUEADA"}")
                blocked
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error leyendo status de bloqueo: ${e.message}")
            false 
        }
    }

    /**
     * Ejecuta una escritura física en la tarjeta para cambiar su estado a Bloqueado.
     *
     * @return Verdadero si la operación fue confirmada por el chip.
     */
    fun blockCard(): Boolean {
        return try {
            // Preparación del bloque de datos con el identificador de archivo y el valor de bloqueo.
            val statusData = ByteArray(7 + 1)
            statusData[0] = FILE_STATUS
            statusData[1] = 0x00
            statusData[2] = 0x00
            statusData[3] = 0x00
            statusData[4] = 0x01
            statusData[5] = 0x00
            statusData[6] = 0x00
            statusData[7] = STATUS_BLOCKED

            val success = desfire.writeData(statusData)
            if (success) {
                Log.d(TAG, "✅ Tarjeta $uidHex BLOQUEADA exitosamente")
            } else {
                Log.e(TAG, "❌ Error al bloquear tarjeta $uidHex")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en blockCard: ${e.message}")
            false
        }
    }

    /**
     * Ejecuta una escritura física en la tarjeta para cambiar su estado a Desbloqueado.
     *
     * @return Verdadero si la operación fue exitosa.
     */
    fun unblockCard(): Boolean {
        return try {
            val statusData = ByteArray(7 + 1)
            statusData[0] = FILE_STATUS
            statusData[1] = 0x00
            statusData[2] = 0x00
            statusData[3] = 0x00
            statusData[4] = 0x01
            statusData[5] = 0x00
            statusData[6] = 0x00
            statusData[7] = STATUS_UNBLOCKED

            val success = desfire.writeData(statusData)
            if (success) {
                Log.d(TAG, "✅ Tarjeta $uidHex DESBLOQUEADA exitosamente")
            } else {
                Log.e(TAG, "❌ Error al desbloquear tarjeta $uidHex")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en unblockCard: ${e.message}")
            false
        }
    }

    /**
     * Ejecuta la lógica de verificación integral:
     * 1. Verifica bloqueo local físico.
     * 2. Verifica presencia en lista negra de servidor.
     * 3. Ejecuta bloqueo físico si se encuentra en lista negra.
     *
     * @param db Instancia de la base de datos para consulta de blacklist.
     * @return Resultado detallado de la verificación [VerificacionResultado].
     */
    suspend fun verificarYBloquearSiNecesario(db: AppDatabase): VerificacionResultado {
        if (isCardBlocked()) {
            Log.w(TAG, "🚫 CASO 1: Tarjeta $uidHex ya está BLOQUEADA localmente")
            return VerificacionResultado.rechazada("TARJETA BLOQUEADA", "Tarjeta bloqueada permanentemente")
        }

        val blacklistEntry = db.blacklistDao().findByUuid(uidHex)

        if (blacklistEntry != null) {
            Log.w(TAG, "🚫 CASO 2: Tarjeta $uidHex encontrada en BLACKLIST - Bloqueando localmente...")

            val bloqueoExitoso = blockCard()

            if (bloqueoExitoso) {
                Log.i(TAG, "✅ Tarjeta $uidHex bloqueada localmente por blacklist")
            } else {
                Log.e(TAG, "❌ No se pudo bloquear localmente la tarjeta $uidHex")
            }

            val numeroTarjeta = blacklistEntry.number ?: uidHex.take(8)

            return VerificacionResultado.rechazada(
                "TARJETA EN BLACKLIST",
                "Tarjeta reportada como perdida/bloqueada",
                numeroTarjeta
            )
        }

        Log.d(TAG, "✅ CASO 3: Tarjeta $uidHex válida - Continuando con cobro")
        return VerificacionResultado.aprobada()
    }

    /**
     * Modelo que representa el resultado de un proceso de verificación de integridad.
     */
    class VerificacionResultado private constructor(
        /** Indica si la tarjeta superó todas las pruebas de seguridad. */
        val isAprobada: Boolean,
        /** Título del mensaje de error si fue rechazada. */
        val errorMensaje: String? = null,
        /** Detalle técnico del porqué del rechazo. */
        val errorDetalle: String? = null,
        /** Número de tarjeta asociado al evento. */
        val numeroTarjeta: String? = null
    ) {
        companion object {
            /** Genera un resultado exitoso. */
            fun aprobada() = VerificacionResultado(true)
            /** Genera un resultado de rechazo con información descriptiva. */
            fun rechazada(mensaje: String, detalle: String, numero: String? = null) =
                VerificacionResultado(false, mensaje, detalle, numero)
        }
    }
}
