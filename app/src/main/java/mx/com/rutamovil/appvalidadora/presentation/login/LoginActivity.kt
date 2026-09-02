package mx.com.rutamovil.appvalidadora.presentation.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mx.com.rutamovil.appvalidadora.R
import mx.com.rutamovil.appvalidadora.presentation.ViewModelFactory
import mx.com.rutamovil.appvalidadora.presentation.main.MainActivity

/**
 * Actividad encargada de la interfaz de inicio de sesión para los operadores del validador.
 * Gestiona la entrada de credenciales, validación de sesión persistente y transiciones de estado de UI.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var editTextUsuario: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonIngresar: Button
    private lateinit var loginMainLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Verificación inmediata de existencia de una sesión válida previa.
        if (verificarSesionActiva()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        // Configuración de la lógica de negocio a través del ViewModel.
        val factory = ViewModelFactory(this.applicationContext)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        inicializarVistas()
        observarEstado()

        // Registro del evento de clic para iniciar la autenticación.
        buttonIngresar.setOnClickListener {
            val email = editTextUsuario.text.toString()
            val pass = editTextPassword.text.toString()
            viewModel.login(email, pass)
        }
    }

    /** Vincula las variables locales con los componentes definidos en el layout XML. */
    private fun inicializarVistas() {
        editTextUsuario = findViewById(R.id.textInputEditTextUsuario)
        editTextPassword = findViewById(R.id.textInputEditTextPassword)
        buttonIngresar = findViewById(R.id.button)
        loginMainLayout = findViewById(R.id.main)
    }

    /**
     * Consulta las preferencias compartidas para determinar si existe un token JWT válido
     * y si este no ha superado el tiempo de expiración definido (6 meses).
     *
     * @return Verdadero si la sesión es válida y está vigente.
     */
    private fun verificarSesionActiva(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)
        val loginTime = prefs.getLong("login_timestamp", 0)

        Log.d("SESSION", "Verificando sesión activa...")
        Log.d("SESSION", "Token guardado: ${token?.take(20)}...")
        Log.d("SESSION", "Fecha login: ${java.util.Date(loginTime)}")

        if (token.isNullOrBlank()) return false

        // Lógica de vigencia máxima: 6 meses (aprox. 180 días).
        val seisMesesMs = 15552000000L 
        val tiempoTranscurrido = System.currentTimeMillis() - loginTime
        
        return tiempoTranscurrido < seisMesesMs
    }

    /**
     * Suscribe la UI a los cambios de estado emitidos por el ViewModel.
     * Actualiza la habilitación de controles y muestra mensajes de error/éxito.
     */
    private fun observarEstado() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginState.Idle -> {
                        buttonIngresar.isEnabled = true
                        buttonIngresar.text = "INGRESAR"
                    }
                    is LoginState.Loading -> {
                        buttonIngresar.isEnabled = false
                        buttonIngresar.text = "VERIFICANDO..."
                        loginMainLayout.setBackgroundColor(Color.LTGRAY)
                    }
                    is LoginState.Success -> {
                        Toast.makeText(this@LoginActivity, "✅ Sesión iniciada", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    is LoginState.Error -> {
                        buttonIngresar.isEnabled = true
                        buttonIngresar.text = "INGRESAR"
                        loginMainLayout.setBackgroundColor(Color.WHITE)
                        Toast.makeText(this@LoginActivity, "❌ ${state.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
