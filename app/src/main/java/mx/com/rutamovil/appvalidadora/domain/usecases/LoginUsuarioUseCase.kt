package mx.com.rutamovil.appvalidadora.domain.usecases

import mx.com.rutamovil.appvalidadora.domain.repositories.IAuthRepository

/**
 * Caso de uso encargado de gestionar el inicio de sesión de los operadores.
 * Valida la presencia de credenciales y delega la ejecución al repositorio de autenticación.
 *
 * @property authRepository Repositorio de autenticación para validar las credenciales.
 */
class LoginUsuarioUseCase(private val authRepository: IAuthRepository) {

    /**
     * Ejecuta la lógica de inicio de sesión.
     * Implementado como operador de invocación para facilitar su uso sintáctico.
     *
     * @param email Correo electrónico proporcionado por el usuario.
     * @param password Contraseña proporcionada.
     * @return Verdadero si el servidor autorizó el acceso, falso si las credenciales son inválidas o están vacías.
     */
    suspend operator fun invoke(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            return false
        }
        return authRepository.login(email, password)
    }
}
