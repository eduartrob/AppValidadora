package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.session

import mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.Desfire3K3DESKey
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Implementación de la llave de sesión para Triple DES de 3 llaves (3K3DES, 192 bits).
 * Proporciona el máximo nivel de seguridad para aplicaciones DESFire basadas en DES.
 */
class Desfire3K3DESSessionKey : DesfireSessionKey<Desfire3K3DESKey> {

    private lateinit var decodeCiphers: Array<Cipher>
    private lateinit var encodeCiphers: Array<Cipher>

    constructor() : super()

    /** Crea una nueva instancia de llave de sesión forzando el formato de 24 bytes. */
    @Throws(Exception::class)
    fun newInstance(value: ByteArray): Desfire3K3DESSessionKey {
        val data = ByteArray(24)
        System.arraycopy(value, 0, data, 0, 24)
        for (n in 0 until 8) {
            data[n] = (data[n].toInt() and 0xFE).toByte()
        }

        return Desfire3K3DESSessionKey(data)
    }

    /** Inicializa la cadena de cifrado E-D-E con tres componentes de llave independientes. */
    @Throws(Exception::class)
    protected constructor(data: ByteArray) {
        this.data = data

        val secretKeySpec = arrayOf(
            SecretKeySpec(data, 0, 8, "DES"),
            SecretKeySpec(data, 8, 8, "DES"),
            SecretKeySpec(data, 16, 8, "DES")
        )

        decodeCiphers = arrayOf(
            Cipher.getInstance("DES/ECB/NoPadding"),
            Cipher.getInstance("DES/ECB/NoPadding"),
            Cipher.getInstance("DES/ECB/NoPadding")
        )

        encodeCiphers = arrayOf(
            Cipher.getInstance("DES/ECB/NoPadding"),
            Cipher.getInstance("DES/ECB/NoPadding"),
            Cipher.getInstance("DES/ECB/NoPadding")
        )

        encodeCiphers[0].init(Cipher.ENCRYPT_MODE, secretKeySpec[0])
        encodeCiphers[1].init(Cipher.DECRYPT_MODE, secretKeySpec[1])
        encodeCiphers[2].init(Cipher.ENCRYPT_MODE, secretKeySpec[2])

        decodeCiphers[0].init(Cipher.DECRYPT_MODE, secretKeySpec[0])
        decodeCiphers[1].init(Cipher.ENCRYPT_MODE, secretKeySpec[1])
        decodeCiphers[2].init(Cipher.DECRYPT_MODE, secretKeySpec[2])
    }

    @Throws(Exception::class)
    override fun encrypt(payload: ByteArray): ByteArray {
        var result = payload
        for (cipher in encodeCiphers) {
            result = cipher.doFinal(result)
        }
        return result
    }

    @Throws(Exception::class)
    override fun decrypt(payload: ByteArray): ByteArray {
        var result = payload
        for (cipher in decodeCiphers) {
            result = cipher.doFinal(result)
        }
        return result
    }

    /** Deriva la llave de sesión de 24 bytes a partir de los números aleatorios A y B. */
    @Throws(Exception::class)
    override fun newKey(rnda: ByteArray, rndb: ByteArray): Desfire3K3DESSessionKey {
        val buffer = ByteArray(24)

        System.arraycopy(rnda, 0, buffer, 0, 4)
        System.arraycopy(rndb, 0, buffer, 4, 4)
        System.arraycopy(rnda, 6, buffer, 8, 4)
        System.arraycopy(rndb, 6, buffer, 12, 4)
        System.arraycopy(rnda, 12, buffer, 16, 4)
        System.arraycopy(rndb, 12, buffer, 20, 4)

        return newInstance(buffer)
    }
}
