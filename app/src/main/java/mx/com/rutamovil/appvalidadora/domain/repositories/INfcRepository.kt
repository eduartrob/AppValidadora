package mx.com.rutamovil.appvalidadora.domain.repositories

/**
 * Interfaz para la interacción con el hardware NFC y el chip MIFARE DESFire.
 * Define las capacidades requeridas para la validación y cobro físico de tarjetas.
 */
interface INfcRepository {
    /** Establece comunicación con una tarjeta detectada. */
    fun connect(tag: Any): Boolean
    /** Finaliza la sesión de comunicación con el hardware. */
    fun disconnect()

    /**
     * Autentica el acceso a una aplicación del chip.
     *
     * @param aid Identificador de aplicación.
     * @param keyHex Llave de acceso en formato hexadecimal.
     * @return Verdadero si la autenticación es aceptada.
     */
    @Throws(Exception::class)
    suspend fun authenticate(aid: String, keyHex: String): Boolean

    /** Lee el saldo monetario actual del chip. */
    @Throws(Exception::class)
    suspend fun readBalance(): Double

    /** Obtiene el código de rol persistido en la tarjeta. */
    @Throws(Exception::class)
    suspend fun readRole(): Int

    /** Verifica si la tarjeta tiene un bloqueo físico activo. */
    @Throws(Exception::class)
    suspend fun isCardBlocked(): Boolean

    /** Escribe el estado de bloqueo permanente en la tarjeta. */
    @Throws(Exception::class)
    suspend fun blockCardPhysically(): Boolean

    /**
     * Realiza un descuento de saldo en la tarjeta física.
     *
     * @param monto Cantidad monetaria a debitar.
     * @return Verdadero si la transacción fue confirmada por el chip.
     */
    @Throws(Exception::class)
    suspend fun debitBalance(monto: Double): Boolean
}
