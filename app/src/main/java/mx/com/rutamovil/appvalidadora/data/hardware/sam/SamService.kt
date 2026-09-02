package mx.com.rutamovil.appvalidadora.data.hardware.sam

import android.content.Context
import java.security.MessageDigest
import java.util.Arrays

/**
 * Servicio encargado de emular las funciones de un Módulo de Acceso Seguro (SAM).
 * Proporciona lógica para la derivación de llaves criptográficas únicas por tarjeta
 * a partir de un secreto maestro y el identificador físico (UID).
 *
 * @property context Contexto de la aplicación.
 */
class SamService(private val context: Context) {

    /** 
     * Llave Maestra Raíz (Simulada para pruebas). 
     * En una implementación real, esta llave nunca debería estar en código claro.
     */
    private val DUMMY_MASTER_KEY_HEX = "4F8AE211B93CD0557A9122F40C668B1D"

    /**
     * Deriva una llave de 128 bits específica para una tarjeta utilizando SHA-256.
     * Combina la llave raíz, el UID de la tarjeta y un diversificador (identificador de llave).
     *
     * @param uid Identificador físico único de la tarjeta.
     * @param keyIdentifier Cadena que identifica el propósito de la llave (ej. "MASTER", "DEBIT").
     * @return Arreglo de 16 bytes con la llave diversificada resultante.
     */
    fun deriveKey(uid: ByteArray, keyIdentifier: String): ByteArray {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")

            // Incorporación de la llave maestra simétrica al resumen.
            digest.update(mx.com.rutamovil.appvalidadora.common.util.Utils.hexStringToByteArray(DUMMY_MASTER_KEY_HEX))
            // Incorporación de la identidad física de la tarjeta.
            digest.update(uid)
            // Incorporación del contexto de la llave.
            digest.update(keyIdentifier.toByteArray())

            val hash = digest.digest()
            // Se toman los primeros 16 bytes del hash SHA-256 para formar una llave AES-128.
            Arrays.copyOfRange(hash, 0, 16)
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(16) // Retorna llave en ceros ante errores críticos para evitar fallos de ejecución.
        }
    }
}
