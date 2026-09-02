package mx.com.rutamovil.appvalidadora.data.hardware.nfc.crypto

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec

/**
 * Objeto de utilidad para el cifrado y descifrado heredado mediante el algoritmo DES.
 */
object DES {
    /**
     * Cifra un bloque de datos utilizando DES en modo CBC sin relleno.
     *
     * @param myIV Vector de inicialización.
     * @param myKey Llave DES de 8 bytes.
     * @param myMsg Mensaje a cifrar.
     * @return Arreglo con el criptograma o nulo en caso de excepción.
     */
    fun encrypt(myIV: ByteArray, myKey: ByteArray, myMsg: ByteArray): ByteArray? {
        return try {
            val iv = IvParameterSpec(myIV)
            val desKey = DESKeySpec(myKey)
            val keyFactory = SecretKeyFactory.getInstance("DES")
            val key = keyFactory.generateSecret(desKey)
            val cipher = Cipher.getInstance("DES/CBC/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)
            cipher.doFinal(myMsg)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Descifra datos utilizando DES en modo CBC.
     *
     * @param myIV Vector de inicialización.
     * @param myKey Llave DES.
     * @param myMsg Criptograma a descifrar.
     * @return Arreglo con los datos en claro.
     */
    fun decrypt(myIV: ByteArray, myKey: ByteArray, myMsg: ByteArray, offset: Int = 0, length: Int = myMsg.size): ByteArray? {
        return try {
            val iv = IvParameterSpec(myIV)
            val desKey = DESKeySpec(myKey)
            val keyFactory = SecretKeyFactory.getInstance("DES")
            val key = keyFactory.generateSecret(desKey)
            val cipher = Cipher.getInstance("DES/CBC/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            cipher.doFinal(myMsg, offset, length)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
