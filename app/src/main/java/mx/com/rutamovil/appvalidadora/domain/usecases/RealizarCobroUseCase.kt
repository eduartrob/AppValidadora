package mx.com.rutamovil.appvalidadora.domain.usecases

import android.util.Log
import mx.com.rutamovil.appvalidadora.common.Constants
import mx.com.rutamovil.appvalidadora.data.local.AppDatabase
import mx.com.rutamovil.appvalidadora.data.local.entity.TransactionEntity
import mx.com.rutamovil.appvalidadora.domain.helpers.ControlCortesHelper
import mx.com.rutamovil.appvalidadora.domain.repositories.INfcRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Caso de uso central encargado de orquestar el proceso completo de cobro sobre una tarjeta física.
 * Realiza la conexión, autenticación, validación de integridad (blacklist/bloqueo),
 * validación de tarifa según rol y finalmente el débito monetario.
 *
 * @property nfcRepository Repositorio para la comunicación con el hardware NFC.
 * @property db Instancia de la base de datos local para validación y persistencia.
 * @property cortesHelper Asistente para la gestión de tarifas y normalización de perfiles.
 */
class RealizarCobroUseCase(
    private val nfcRepository: INfcRepository,
    private val db: AppDatabase,
    private val cortesHelper: ControlCortesHelper
) {
    /**
     * Ejecuta el flujo de cobro.
     *
     * @param tag Objeto de etiqueta NFC detectada.
     * @param uidHex Identificador único de la tarjeta en formato hexadecimal.
     * @param aid Identificador de aplicación DESFire a acceder.
     * @param adminKeyHex Llave administrativa para la autenticación en el chip.
     * @param categoriaBoton Perfil de usuario seleccionado en la interfaz (ej. "ESTUDIANTE").
     * @param esMinimo Indica si se debe aplicar la tarifa mínima o máxima del perfil.
     * @return Un Triple conteniendo:
     *         1. Booleano: Éxito o fracaso de la operación.
     *         2. String: Mensaje descriptivo del error o saldo restante en caso de éxito.
     *         3. Double: El monto total que fue cobrado.
     */
    suspend operator fun invoke(
        tag: Any,
        uidHex: String,
        aid: String,
        adminKeyHex: String,
        categoriaBoton: String?,
        esMinimo: Boolean
    ): Triple<Boolean, String, Double> {
        try {
            // 1. Verificación de selección previa de tarifa.
            if (categoriaBoton == null) {
                return Triple(false, "⚠️ SELECCIONE TARIFA PRIMERO", 0.0)
            }

            // Establecimiento de conexión física.
            if (!nfcRepository.connect(tag)) {
                return Triple(false, "No se pudo conectar con la tarjeta.", 0.0)
            }

            // 2. Proceso de autenticación mutua con el chip.
            if (!nfcRepository.authenticate(aid, adminKeyHex)) {
                return Triple(false, "Tarjeta Inválida (Autenticación Fallida)", 0.0)
            }

            // 3. Verificación de integridad y seguridad de la tarjeta.
            if (nfcRepository.isCardBlocked()) {
                return Triple(false, "TARJETA BLOQUEADA. Retenida por seguridad.", 0.0)
            }

            val enBlacklist = db.blacklistDao().findByUuid(uidHex)
            if (enBlacklist != null) {
                // Bloqueo físico inmediato si la tarjeta está reportada en la lista negra.
                nfcRepository.blockCardPhysically()
                return Triple(false, "TARJETA EN LISTA NEGRA. Ha sido bloqueada.", 0.0)
            }

            // 4. Validación de consistencia entre el rol de la tarjeta y el botón presionado.
            val rolCode = nfcRepository.readRole()
            val rolTarjeta = getRoleName(rolCode)
            val rNorm = cortesHelper.normalizar(rolTarjeta)
            val bNorm = cortesHelper.normalizar(categoriaBoton ?: "")
            
            if (rNorm != bNorm) {
                return Triple(false, "❌ ERROR: Tarjeta $rolTarjeta ($rNorm) - Botón $categoriaBoton ($bNorm)", 0.0)
            }

            // 5. Resolución de la tarifa dinámica aplicable.
            val monto = cortesHelper.getTarifaDesdeCache(categoriaBoton ?: "", esMinimo)
            if (monto <= 0.0) {
                val todas = cortesHelper.obtenerTarifasCache()
                val tiposEnBD = todas.map { "${it.passenger_type}(${cortesHelper.normalizar(it.passenger_type ?: "")})" }.distinct().joinToString(", ")
                return Triple(false, "Error: Tarifa no encontrada para $categoriaBoton ($bNorm).\nEn BD hay: $tiposEnBD", 0.0)
            }

            // 6. Verificación de fondos disponibles en el chip.
            val saldoActual = nfcRepository.readBalance()
            if (saldoActual < monto) {
                return Triple(false, "SALDO INSUFICIENTE. Saldo actual: $$saldoActual", 0.0)
            }

            // 7. Ejecución de la transacción atómica en el hardware.
            val exito = nfcRepository.debitBalance(monto)
            if (!exito) {
                return Triple(false, "Fallo al descontar el saldo.", 0.0)
            }

            // 8. Registro de la transacción local para su posterior envío al servidor.
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val fechaActual = sdf.format(Date())

            val transaction = TransactionEntity(
                cardUuid = uidHex,
                passengerType = categoriaBoton,
                amount = monto,
                description = "Cobro $categoriaBoton",
                chargedAt = fechaActual,
                synced = false,
                deviceUptimeMs = android.os.SystemClock.elapsedRealtime()
            )

            db.transactionDao().insert(transaction)
            Log.d("COBRO", "Transacción guardada localmente para sync: $uidHex - $$monto")

            // 9. Recuperación del saldo final para confirmación visual.
            val nuevoSaldo = nfcRepository.readBalance()
            return Triple(true, nuevoSaldo.toString(), monto)

        } catch (e: Exception) {
            val msg = e.message ?: "Error desconocido"
            // Manejo específico del código de error 9D (Permission Denied) de MIFARE.
            return if (msg.contains("9d", ignoreCase = true)) {
                Triple(false, "Error de Acceso (9D): Tarjeta no autorizada", 0.0)
            } else {
                Triple(false, "Error de lectura: $msg", 0.0)
            }
        } finally {
            // Asegurar el cierre de la conexión NFC independientemente del resultado.
            nfcRepository.disconnect()
        }
    }

    /**
     * Mapea el código numérico del chip a una etiqueta de rol legible.
     */
    private fun getRoleName(code: Int): String {
        return when (code.toByte()) {
            Constants.ROLE_REGULAR -> "REGULAR"
            Constants.ROLE_ESTUDIANTE -> "ESTUDIANTE"
            Constants.ROLE_TERCERA_EDAD -> "3ERA EDAD"
            Constants.ROLE_PCD -> "DISCAPACITADO"
            else -> "DESCONOCIDO"
        }
    }
}
