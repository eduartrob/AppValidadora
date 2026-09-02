package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.session

import mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.DesfireAESKey
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Implementación de la llave de sesión para algoritmos AES de 128 bits.
 * Gestiona el cifrado y descifrado de tramas utilizando AES en modo ECB sin relleno.
 */
class DesfireAESSessionKey : DesfireSessionKey<DesfireAESKey> {

    private lateinit var decodeCipher: Cipher
    private lateinit var encodeCipher: Cipher

    constructor(key: ByteArray) {
        if (key.size != 16) {
            throw IllegalArgumentException("La llave de sesión AES debe ser de 16 bytes")
        }
        this.data = key

        val secretKeySpec = SecretKeySpec(data, 0, 16, "AES")

        decodeCipher = Cipher.getInstance("AES/ECB/NoPadding")
        decodeCipher.init(Cipher.DECRYPT_MODE, secretKeySpec)

        encodeCipher = Cipher.getInstance("AES/ECB/NoPadding")
        encodeCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
    }

    @Throws(Exception::class)
    override fun encrypt(encrypt: ByteArray): ByteArray {
        return encodeCipher.doFinal(encrypt)
    }

    @Throws(Exception::class)
    override fun decrypt(decrypt: ByteArray): ByteArray {
        return decodeCipher.doFinal(decrypt)
    }

    /**
     * Implementa la derivación de la llave de sesión AES según la especificación DESFire.
     * Combina porciones específicas de RndA y RndB.
     */
    @Throws(Exception::class)
    override fun newKey(rnda: ByteArray, rndb: ByteArray): DesfireAESSessionKey {
        val buffer = ByteArray(16)
        System.arraycopy(rnda, 0, buffer, 0, 4)
        System.arraycopy(rndb, 0, buffer, 4, 4)
        System.arraycopy(rnda, 12, buffer, 8, 4)
        System.arraycopy(rndb, 12, buffer, 12, 4)

        return DesfireAESSessionKey(buffer)
    }
}
