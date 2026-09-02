package mx.com.rutamovil.appvalidadora.data.repositories

import mx.com.rutamovil.appvalidadora.data.remote.APIClient
import mx.com.rutamovil.appvalidadora.domain.repositories.IAuthRepository

/**
 * Implementación del repositorio de autenticación que utiliza un cliente de API para la comunicación con el servidor.
 * Encargado de orquestar los procesos de inicio de sesión y registro de dispositivos/tarjetas.
 *
 * @property apiClient Cliente de red para la ejecución de peticiones HTTP.
 */
class AuthRepositoryImpl(private val apiClient: APIClient) : IAuthRepository {

    /**
     * Realiza la validación de credenciales de usuario ante el servidor central.
     *
     * @param email Correo electrónico institucional.
     * @param password Contraseña del usuario.
     * @return Verdadero si el acceso fue autorizado y el token almacenado, falso en caso contrario.
     */
    override suspend fun login(email: String, password: String): Boolean {
        return apiClient.login(email, password)
    }

    /**
     * Registra una tarjeta física en el backend del sistema tras su inicialización en el hardware.
     *
     * @param uid Identificador físico único de la tarjeta.
     * @param masterKey Llave maestra asignada a la tarjeta.
     * @param systemKey Llave de sistema asignada.
     * @param discountType Tipo de perfil de tarifa o descuento.
     * @return Un par que indica el éxito de la operación y un mensaje descriptivo del resultado.
     */
    override suspend fun registerCardInBackend(
        uid: String,
        masterKey: String,
        systemKey: String,
        discountType: String
    ): Pair<Boolean, String> {
        return apiClient.initializeCard(uid, masterKey, systemKey, discountType)
    }
}
