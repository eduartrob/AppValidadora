package mx.com.rutamovil.appvalidadora.data.hardware.nfc.crypto

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Objeto de utilidad para operaciones criptográficas avanzadas (AES).
 * Proporciona métodos estáticos para cifrado y descifrado en modo CBC sin relleno (NoPadding).
 */
object AES {
    /**
     * Cifra un mensaje utilizando el algoritmo AES en modo CBC.
     *
     * @param iv Vector de inicialización.
     * @param key Llave de cifrado.
     * @param msg Mensaje en claro (debe ser múltiplo del tamaño de bloque si se usa NoPadding).
     * @return Arreglo de bytes con el criptograma o nulo si ocurre un error.
     */
    @Throws(Exception::class)
    fun encrypt(iv: ByteArray, key: ByteArray, msg: ByteArray): ByteArray? {
        return try {
            val ivSpec = IvParameterSpec(iv)
            val sks = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sks, ivSpec)
            cipher.doFinal(msg)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Descifra un criptograma utilizando AES en modo CBC.
     *
     * @param iv Vector de inicialización utilizado en el cifrado.
     * @param key Llave de descifrado.
     * @param msg Criptograma a procesar.
     * @param offset Posición inicial en el arreglo de entrada.
     * @param length Longitud de los datos a descifrar.
     * @return Arreglo de bytes con el mensaje original o nulo en caso de error.
     */
    @Throws(Exception::class)
    fun decrypt(iv: ByteArray, key: ByteArray, msg: ByteArray, offset: Int = 0, length: Int = msg.size): ByteArray? {
        return try {
            val ivSpec = IvParameterSpec(iv)
            val sks = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, sks, ivSpec)
            cipher.doFinal(msg, offset, length)
        } catch (e: Exception) {
            null
        }
    }
}
