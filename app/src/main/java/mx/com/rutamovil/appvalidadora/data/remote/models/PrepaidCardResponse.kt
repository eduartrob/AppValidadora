package mx.com.rutamovil.appvalidadora.data.remote.models

import com.google.gson.annotations.SerializedName

/**
 * Respuesta para la descarga o consulta de información sobre tarjetas prepagadas.
 *
 * @property status Resultado de la operación en el servidor.
 * @property data Listado de tarjetas prepagadas con sus configuraciones.
 */
data class PrepaidCardResponse(
    val status: Boolean,
    val data: List<PrepaidCardData>?
)

/**
 * Información detallada de una tarjeta prepagada registrada en el sistema administrativo.
 * Incluye llaves críticas necesarias para la inicialización física del chip.
 *
 * @property uuid Identificador único de la tarjeta.
 * @property number Número visible de la tarjeta.
 * @property discountType Tipo de perfil de descuento asignado.
 * @property balance Saldo inicial o actual registrado.
 * @property masterKeyPlain Llave maestra en formato legible para configuración del chip.
 * @property systemKeyPlain Llave de sistema en formato legible.
 */
data class PrepaidCardData(
    val uuid: String?,
    val number: String?,
    @SerializedName("discount_type") val discountType: String?,
    val balance: String?,
    @SerializedName("master_key_plain") val masterKeyPlain: String?,
    @SerializedName("system_key_plain") val systemKeyPlain: String?
)
