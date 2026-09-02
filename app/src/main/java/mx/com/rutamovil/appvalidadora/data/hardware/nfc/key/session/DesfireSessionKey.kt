package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.session

import mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.DesfireKey

/**
 * Clase base abstracta para la gestión de llaves de sesión efímeras en DESFire.
 * Una llave de sesión se deriva tras una autenticación exitosa y se utiliza 
 * para cifrar la comunicación posterior durante la vida de la conexión.
 *
 * @param T Tipo de [DesfireKey] que sirve de base para la sesión.
 */
abstract class DesfireSessionKey<T : DesfireKey> {

    /** Objeto de llave original. */
    var key: T? = null
    /** Valor binario derivado de la llave de sesión. */
    var data: ByteArray? = null

    /** Sub-llaves para el cálculo de CMAC (Cipher-based Message Authentication Code). */
    var cmacSK1: ByteArray = ByteArray(24)
    var cmacSK2: ByteArray = ByteArray(24)

    /**
     * Cifra un bloque de datos utilizando la llave de sesión actual.
     */
    @Throws(Exception::class)
    abstract fun encrypt(encrypt: ByteArray): ByteArray

    /**
     * Descifra un bloque de datos utilizando la llave de sesión actual.
     */
    @Throws(Exception::class)
    abstract fun decrypt(decrypt: ByteArray): ByteArray

    /**
     * Deriva una nueva llave de sesión a partir de los números aleatorios compartidos.
     *
     * @param rnda Número aleatorio A generado por el lector.
     * @param rndb Número aleatorio B generado por la tarjeta.
     * @return Nueva instancia de [DesfireSessionKey].
     */
    @Throws(Exception::class)
    abstract fun newKey(rnda: ByteArray, rndb: ByteArray): DesfireSessionKey<T>
}
