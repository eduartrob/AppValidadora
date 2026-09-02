package mx.com.rutamovil.appvalidadora.data.remote.models

/**
 * Modelo de respuesta para la descarga de la lista negra desde el servidor.
 *
 * @property status Indica si la petición fue exitosa para el servidor.
 * @property data Listado de objetos que contienen la información de las tarjetas bloqueadas.
 */
data class BlacklistResponse(
    val status: Boolean,
    val data: List<BlacklistCardData>?
)

/**
 * Datos individuales de una tarjeta bloqueada proporcionados por la API.
 *
 * @property uuid Identificador único de la tarjeta física.
 * @property number Número de serie o identificador visible asociado.
 */
data class BlacklistCardData(
    val uuid: String,
    val number: String?
)
