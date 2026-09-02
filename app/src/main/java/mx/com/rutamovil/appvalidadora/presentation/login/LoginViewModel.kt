package mx.com.rutamovil.appvalidadora.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.com.rutamovil.appvalidadora.domain.usecases.LoginUsuarioUseCase

/**
 * Representación de los estados posibles de la interfaz de inicio de sesión.
 */
sealed class LoginState {
    /** Estado inicial o en espera de interacción. */
    object Idle : LoginState()
    /** Indica que se está procesando una petición de autenticación. */
    object Loading : LoginState()
    /** Indica que el acceso fue autorizado. */
    object Success : LoginState()
    /** 
     * Indica un fallo en el proceso de inicio de sesión.
     * @property message Descripción legible del error.
     */
    data class Error(val message: String) : LoginState()
}

/**
 * ViewModel que gestiona la lógica de estado para la pantalla de Login.
 * Facilita la comunicación entre la vista y el caso de uso de autenticación.
 *
 * @property loginUsuarioUseCase Caso de uso para la validación de credenciales.
 */
class LoginViewModel(
    private val loginUsuarioUseCase: LoginUsuarioUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginState>(LoginState.Idle)
    /** Flujo de estado reactivo expuesto a la vista. */
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    /**
     * Orquesta el proceso de inicio de sesión de forma asíncrona.
     *
     * @param email Correo electrónico institucional.
     * @param pass Contraseña.
     */
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = LoginState.Error("Llene todos los campos")
            return
        }

        _uiState.value = LoginState.Loading

        viewModelScope.launch {
            val success = loginUsuarioUseCase(email, pass)
            if (success) {
                _uiState.value = LoginState.Success
            } else {
                _uiState.value = LoginState.Error("Credenciales incorrectas o error de red")
            }
        }
    }
}
