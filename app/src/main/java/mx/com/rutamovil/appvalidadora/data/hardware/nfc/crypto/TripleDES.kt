package mx.com.rutamovil.appvalidadora.data.hardware.nfc.crypto

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESedeKeySpec
import javax.crypto.spec.IvParameterSpec

/**
 * Objeto encargado de proveer las funcionalidades del algoritmo Triple DES (3DES/DESede).
 * Utilizado para la compatibilidad con aplicaciones DESFire que no utilizan AES.
 */
object TripleDES {
    /**
     * Cifra datos utilizando Triple DES en modo CBC sin relleno.
     *
     * @param myIV Vector de inicialización.
     * @param myKey Llave Triple DES (16 o 24 bytes).
     * @param myMsg Mensaje a cifrar.
     * @return Criptograma resultante o nulo si falla.
     */
    fun encrypt(myIV: ByteArray, myKey: ByteArray, myMsg: ByteArray, offset: Int = 0, length: Int = myMsg.size): ByteArray? {
        return try {
            val iv = IvParameterSpec(myIV)
            val desKey = DESedeKeySpec(myKey)
            val keyFactory = SecretKeyFactory.getInstance("DESede")
            val key = keyFactory.generateSecret(desKey)
            val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)
            cipher.doFinal(myMsg, offset, length)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Descifra datos asumiendo un vector de inicialización nulo por defecto. */
    fun decrypt(myKey: ByteArray, myMsg: ByteArray, offset: Int, length: Int): ByteArray? {
        return decrypt(ByteArray(8), myKey, myMsg, offset, length)
    }

    /**
     * Descifra datos utilizando Triple DES en modo CBC.
     *
     * @param myIV Vector de inicialización.
     * @param myKey Llave Triple DES.
     * @param myMsg Criptograma de entrada.
     * @return Datos descifrados o nulo en caso de error.
     */
    fun decrypt(myIV: ByteArray, myKey: ByteArray, myMsg: ByteArray, offset: Int = 0, length: Int = myMsg.size): ByteArray? {
        return try {
            val iv = IvParameterSpec(myIV)
            val desKey = DESedeKeySpec(myKey)
            val keyFactory = SecretKeyFactory.getInstance("DESede")
            val key = keyFactory.generateSecret(desKey)
            val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            cipher.doFinal(myMsg, offset, length)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
