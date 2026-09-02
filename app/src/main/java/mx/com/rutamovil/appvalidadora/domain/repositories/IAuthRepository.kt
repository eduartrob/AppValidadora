package mx.com.rutamovil.appvalidadora.domain.repositories

/**
 * Interfaz que define las operaciones de autenticación y registro de seguridad.
 * Abstrae la comunicación con los servicios de identidad del backend.
 */
interface IAuthRepository {
    /**
     * Valida las credenciales del usuario validador.
     *
     * @param email Correo electrónico.
     * @param password Contraseña.
     * @return Verdadero si el inicio de sesión es exitoso.
     */
    suspend fun login(email: String, password: String): Boolean

    /**
     * Reporta la inicialización de una nueva tarjeta al servidor central.
     *
     * @param uid Identificador único de la tarjeta.
     * @param masterKey Llave maestra diversificada.
     * @param systemKey Llave de sistema diversificada.
     * @param discountType Tipo de tarifa aplicada.
     * @return Un par conteniendo el estado de éxito y un mensaje informativo.
     */
    suspend fun registerCardInBackend(
        uid: String,
        masterKey: String,
        systemKey: String,
        discountType: String
    ): Pair<Boolean, String>
}
