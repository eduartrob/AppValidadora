package mx.com.rutamovil.appvalidadora.domain.model

/**
 * Catálogo de roles de usuario y sus perfiles de descuento asociados.
 * Mapea los códigos físicos almacenados en la tarjeta con los tipos requeridos por la API remota.
 *
 * @property code Identificador numérico almacenado en el chip.
 * @property apiDiscountType Nombre del perfil de descuento esperado por el backend.
 */
enum class Role(val code: Byte, val apiDiscountType: String) {
    /** Usuario sin descuento especial. */
    REGULAR(0x01, "none"),
    /** Usuario con tarifa de estudiante. */
    ESTUDIANTE(0x02, "student"),
    /** Usuario con tarifa de adulto mayor. */
    TERCERA_EDAD(0x03, "senior"),
    /** Usuario con tarifa para personas con discapacidad. */
    PCD(0x04, "disabled"),
    /** Rol no reconocido o inicial. */
    UNKNOWN(0x00, "none");

    companion object {
        /**
         * Resuelve el rol correspondiente a un código de byte.
         */
        fun fromCode(code: Byte): Role {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}
