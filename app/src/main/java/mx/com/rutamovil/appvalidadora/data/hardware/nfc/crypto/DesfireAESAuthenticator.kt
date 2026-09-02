package mx.com.rutamovil.appvalidadora.data.hardware.nfc.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Clase especializada en la ejecución del protocolo de autenticación mutua AES para tarjetas DESFire.
 * Implementa la lógica de intercambio y rotación de números aleatorios (Random Numbers) 
 * requerida para establecer una sesión segura.
 */
class DesfireAESAuthenticator {

    /**
     * Procesa el primer paso de la autenticación AES.
     * Descifra el desafío de la tarjeta, genera un desafío propio y prepara la respuesta concatenada.
     *
     * @param key Llave de aplicación AES (16 bytes).
     * @param responseData Datos crudos recibidos de la tarjeta en el paso anterior.
     * @return Arreglo cifrado listo para ser enviado como respuesta de autenticación.
     */
    @Throws(Exception::class)
    fun authenticateAES(key: ByteArray, responseData: ByteArray): ByteArray {
        // 1. Descifrar el RndB enviado por la tarjeta.
        val rndB = decryptAES(responseData, key, ByteArray(16))

        // 2. Generar nuestro propio número aleatorio (RndA).
        val rndA = ByteArray(16)
        SecureRandom().nextBytes(rndA)

        // 3. Rotar RndB hacia la izquierda un byte según especificación DESFire.
        val rndBrot = rotateLeft(rndB)

        // 4. Concatenar RndA + RndBrot para formar el bloque de respuesta de 32 bytes.
        val combined = ByteArray(32)
        System.arraycopy(rndA, 0, combined, 0, 16)
        System.arraycopy(rndBrot, 0, combined, 16, 16)

        // 5. Cifrar el bloque resultante utilizando RndB como vector de inicialización.
        return encryptAES(combined, key, responseData)
    }

    /**
     * Utilidad para cifrado AES CBC NoPadding específico para el flujo de autenticación.
     */
    @Throws(Exception::class)
    fun encryptAES(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/CBC/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return c.doFinal(data)
    }

    /**
     * Utilidad para descifrado AES CBC NoPadding específico para el flujo de autenticación.
     */
    @Throws(Exception::class)
    fun decryptAES(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/CBC/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return c.doFinal(data)
    }

    /** Realiza una rotación circular de un byte a la izquierda. */
    private fun rotateLeft(data: ByteArray): ByteArray {
        val rotated = ByteArray(data.size)
        System.arraycopy(data, 1, rotated, 0, data.size - 1)
        rotated[data.size - 1] = data[0]
        return rotated
    }
}
