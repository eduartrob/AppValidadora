package mx.com.rutamovil.appvalidadora.data.remote.models

/**
 * Respuesta genérica de la API tras realizar operaciones sobre tarjetas (ej. cobros).
 *
 * @property status Resultado lógico de la operación en el servidor.
 * @property data Información detallada de la tarjeta afectada.
 */
data class CardsResponse(
    val status: Boolean,
    val data: CardData?
) {
    /**
     * Estructura que contiene los datos de la tarjeta tras una operación exitosa.
     *
     * @property uuid Identificador único de la tarjeta.
     * @property number Número visible de la tarjeta.
     * @property balance Saldo final registrado en el servidor tras la operación.
     */
    data class CardData(
        val uuid: String?,
        val number: String?,
        val balance: String?
    )
}
