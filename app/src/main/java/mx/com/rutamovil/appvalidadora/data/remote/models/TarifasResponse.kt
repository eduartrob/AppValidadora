package mx.com.rutamovil.appvalidadora.data.remote.models

import com.google.gson.annotations.SerializedName

/**
 * Respuesta que contiene el catálogo de tarifas disponibles para el validador.
 *
 * @property status Indica si se obtuvo el catálogo correctamente.
 * @property data Listado de objetos de tarifa [Fare].
 */
data class TarifasResponse(
    val status: Boolean,
    val data: List<Fare>
)

/**
 * Define una tarifa específica basada en el tipo de pasajero.
 *
 * @property id Identificador único de la tarifa.
 * @property fare Nombre descriptivo de la tarifa.
 * @property passenger_type Código o nombre del tipo de pasajero (ej. "Estudiante").
 * @property price Precio monetario formateado como cadena de texto.
 */
data class Fare(
    val id: Int,
    val fare: String?,
    @SerializedName("passenger_type") val passenger_type: String?,
    val price: String?
)
