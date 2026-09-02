package mx.com.rutamovil.appvalidadora.data.remote.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo de respuesta para el proceso de autenticación de usuarios.
 * Contiene el token de acceso y la información del perfil del trabajador.
 *
 * @property status Indica si las credenciales fueron aceptadas.
 * @property data Información extendida del inicio de sesión.
 */
data class LoginResponse(
    val status: Boolean,
    val data: LoginData?
) {
    /**
     * Estructura que agrupa el token y los datos del usuario autenticado.
     *
     * @property accessToken Token JWT (JSON Web Token) para autorizar peticiones subsecuentes.
     * @property tokenType Tipo de token (generalmente "Bearer").
     * @property expiresIn Tiempo de vida del token en segundos.
     * @property user Objeto con los datos personales del usuario.
     * @property role Identificador del rol de usuario en formato cadena.
     */
    data class LoginData(
        @SerializedName("access_token") val accessToken: String?,
        @SerializedName("token_type") val tokenType: String?,
        @SerializedName("expires_in") val expiresIn: Int,
        val user: User?,
        val role: String?
    )

    /**
     * Datos personales y de acceso del usuario autenticado.
     *
     * @property id Identificador numérico único del usuario en el servidor.
     * @property name Nombre(s) del trabajador.
     * @property firstLastName Primer apellido.
     * @property secondLastName Segundo apellido.
     * @property email Dirección de correo electrónico.
     * @property roles Listado de etiquetas de rol asignadas.
     */
    data class User(
        val id: Int,
        val name: String?,
        @SerializedName("first_last_name") val firstLastName: String?,
        @SerializedName("second_last_name") val secondLastName: String?,
        val email: String?,
        val roles: List<String>?
    )

    /**
     * Método de conveniencia para extraer el token de acceso.
     *
     * @return El token JWT o nulo si no está presente.
     */
    fun getToken(): String? = data?.accessToken

    /**
     * Método de conveniencia para extraer el rol del usuario.
     *
     * @return El nombre del rol o nulo.
     */
    fun getRole(): String? = data?.role
}
