package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.session

import mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.DesfireDESKey
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Implementación de la llave de sesión para el algoritmo DES estándar.
 * Gestiona el ciclo de vida de la seguridad para sesiones basadas en llaves de 8 bytes.
 */
class DesfireDESSessionKey : DesfireSessionKey<DesfireDESKey> {

    private lateinit var decodeCipher: Cipher
    private lateinit var encodeCipher: Cipher

    constructor() : super()

    /**
     * Crea una nueva instancia de la llave de sesión aplicando los bits de versión.
     *
     * @param value Valor base de la llave.
     * @param version Byte de versión a integrar en la llave.
     * @return Instancia configurada de [DesfireDESSessionKey].
     */
    @Throws(Exception::class)
    fun newInstance(value: ByteArray, version: Byte): DesfireDESSessionKey {
        val data = ByteArray(8)
        System.arraycopy(value, 0, data, 0, 8)

        for (n in 0 until 8) {
            val versionBit = ((version.toInt() and (1 shl (7 - n))) shr (7 - n)).toByte()

            data[n] = (data[n].toInt() and 0xFE).toByte()
            data[n] = (data[n].toInt() or versionBit.toInt()).toByte()
        }

        return DesfireDESSessionKey(data)
    }

    /** Constructor interno que inicializa los motores de cifrado. */
    @Throws(Exception::class)
    protected constructor(data: ByteArray) {
        this.data = data

        val secretKeySpec = SecretKeySpec(data, 0, 8, "DES")

        decodeCipher = Cipher.getInstance("DES/ECB/NoPadding")
        decodeCipher.init(Cipher.DECRYPT_MODE, secretKeySpec)

        encodeCipher = Cipher.getInstance("DES/ECB/NoPadding")
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

    /** Deriva la llave de sesión DES concatenando porciones de los desafíos aleatorios. */
    @Throws(Exception::class)
    override fun newKey(rnda: ByteArray, rndb: ByteArray): DesfireDESSessionKey {
        val buffer = ByteArray(8)

        System.arraycopy(rnda, 0, buffer, 0, 4)
        System.arraycopy(rndb, 0, buffer, 4, 4)

        return DesfireDESSessionKey(buffer)
    }
}
