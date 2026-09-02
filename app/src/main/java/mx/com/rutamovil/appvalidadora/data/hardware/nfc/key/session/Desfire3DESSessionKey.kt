package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.session

import mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.Desfire3DESKey
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.inv

/**
 * Implementación de la llave de sesión para Triple DES (2-Key 3DES).
 * Emula el comportamiento de hardware del chip DESFire realizando operaciones encadenadas.
 */
class Desfire3DESSessionKey : DesfireSessionKey<Desfire3DESKey> {

    private lateinit var decodeCiphers: Array<Cipher>
    private lateinit var encodeCiphers: Array<Cipher>

    constructor() : super()

    /**
     * Integra el byte de versión en la llave 3DES según el estándar MIFARE.
     */
    @Throws(Exception::class)
    fun newInstance(value: ByteArray, version: Byte): Desfire3DESSessionKey {
        val data = ByteArray(16)
        System.arraycopy(value, 0, data, 0, 16)

        for (n in 0 until 8) {
            val versionBit = ((version.toInt() and (1 shl (7 - n))) shr (7 - n)).toByte()

            data[n] = (data[n].toInt() and 0xFE).toByte()
            data[n] = (data[n].toInt() or versionBit.toInt()).toByte()

            // Escribe la versión invertida en la segunda llave para evitar degradación a DES simple.
            data[n + 8] = (data[n + 8].toInt() and 0xFE).toByte()
            data[n + 8] = (data[n + 8].toInt() or versionBit.inv().toInt()).toByte()
        }

        return Desfire3DESSessionKey(data)
    }

    /** Inicializa el esquema de cifrado E-D-E (Encrypt-Decrypt-Encrypt) propio de 3DES. */
    @Throws(Exception::class)
    protected constructor(value: ByteArray) {
        this.data = value

        val secretKeySpec = arrayOf(
            SecretKeySpec(data, 0, 8, "DES"),
            SecretKeySpec(data, 8, 8, "DES")
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
        encodeCiphers[2].init(Cipher.ENCRYPT_MODE, secretKeySpec[0])

        decodeCiphers[0].init(Cipher.DECRYPT_MODE, secretKeySpec[0])
        decodeCiphers[1].init(Cipher.ENCRYPT_MODE, secretKeySpec[1])
        decodeCiphers[2].init(Cipher.DECRYPT_MODE, secretKeySpec[0])
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

    /** Deriva la llave de sesión 3DES a partir de los números aleatorios A y B. */
    @Throws(Exception::class)
    override fun newKey(rnda: ByteArray, rndb: ByteArray): Desfire3DESSessionKey {
        val buffer = ByteArray(16)

        System.arraycopy(rnda, 0, buffer, 0, 4)
        System.arraycopy(rndb, 0, buffer, 4, 4)
        System.arraycopy(rnda, 4, buffer, 8, 4)
        System.arraycopy(rndb, 4, buffer, 12, 4)

        return Desfire3DESSessionKey(buffer)
    }
}
