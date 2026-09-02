package mx.com.rutamovil.appvalidadora.common

/**
 * Objeto que centraliza las constantes globales utilizadas en la aplicación.
 * Define valores para identificación, seguridad, protocolos de tarjeta DESFire y configuración de red.
 */
object Constants {
    /** Identificador base utilizado para todos los roles de usuario. */
    const val ID_BASE = "123456"

    /** Llave maestra hexadecimal para procesos de autenticación y cifrado. */
    const val MASTER_KEY_HEX = "00000000000000000000000000000000"

    /** Identificador del archivo de saldo en la tarjeta DESFire. */
    const val FILE_VALUE: Byte = 0x01

    /** Identificador del archivo de contador diario en la tarjeta DESFire. */
    const val FILE_COUNTER: Byte = 0x02

    /** Identificador del archivo de fecha de activación en la tarjeta DESFire. */
    const val FILE_ACTIVATION_DATE: Byte = 0x03

    /** Identificador del archivo de rol de usuario en la tarjeta DESFire. */
    const val FILE_ROLE: Byte = 0x04

    /** Identificador del archivo de estado de bloqueo en la tarjeta DESFire. */
    const val FILE_STATUS: Byte = 0x05

    /** Tamaño en bytes del archivo de rol. */
    const val ROLE_FILE_SIZE = 1

    /** Tamaño en bytes del archivo de contador. */
    const val COUNTER_FILE_SIZE = 4

    /** Representación de estado desbloqueado para una tarjeta. */
    const val STATUS_UNBLOCKED: Byte = 0x00

    /** Representación de estado bloqueado para una tarjeta. */
    const val STATUS_BLOCKED: Byte = 0x01

    /** Código de rol para usuarios regulares. */
    const val ROLE_REGULAR: Byte = 0x01

    /** Código de rol para estudiantes. */
    const val ROLE_ESTUDIANTE: Byte = 0x02

    /** Código de rol para personas de la tercera edad. */
    const val ROLE_TERCERA_EDAD: Byte = 0x03

    /** Código de rol para personas con discapacidad. */
    const val ROLE_PCD: Byte = 0x04

    /** URL base de la interfaz de programación de aplicaciones (API) para el entorno de pruebas. */
    const val API_BASE_URL = "https://rmpay-staging.rutamovil.com.mx/api/"
}
