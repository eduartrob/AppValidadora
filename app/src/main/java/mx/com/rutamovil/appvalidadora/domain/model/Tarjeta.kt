package mx.com.rutamovil.appvalidadora.domain.model

/**
 * Modelo de dominio que representa la información de una tarjeta de usuario.
 * Utilizado para la lógica de negocio y transferencia entre capas de datos y presentación.
 *
 * @property uid Identificador único físico de la tarjeta (NFC UID).
 * @property aid Identificador de la aplicación de transporte en la tarjeta.
 * @property masterKey Llave maestra hexadecimal configurada en el chip.
 * @property adminKey Llave de administración para cambios de configuración.
 * @property roleCode Código del rol asignado (estudiante, regular, etc.).
 * @property createdAt Fecha de creación del registro.
 * @property isSynced Indica si el registro ya fue reportado exitosamente al servidor.
 */
data class Tarjeta(
    val uid: String,
    val aid: String,
    val masterKey: String,
    val adminKey: String,
    val roleCode: Byte,
    val createdAt: Long,
    val isSynced: Boolean
)
