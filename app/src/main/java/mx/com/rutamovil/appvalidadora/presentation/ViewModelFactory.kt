package mx.com.rutamovil.appvalidadora.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import mx.com.rutamovil.appvalidadora.data.hardware.sam.SamService
import mx.com.rutamovil.appvalidadora.data.local.AppDatabase
import mx.com.rutamovil.appvalidadora.data.remote.APIClient
import mx.com.rutamovil.appvalidadora.data.repositories.AuthRepositoryImpl
import mx.com.rutamovil.appvalidadora.data.repositories.NfcRepositoryImpl
import mx.com.rutamovil.appvalidadora.domain.helpers.ControlCortesHelper
import mx.com.rutamovil.appvalidadora.domain.usecases.LoginUsuarioUseCase
import mx.com.rutamovil.appvalidadora.domain.usecases.RealizarCobroUseCase
import mx.com.rutamovil.appvalidadora.presentation.login.LoginViewModel
import mx.com.rutamovil.appvalidadora.presentation.main.MainViewModel

/**
 * Fábrica centralizada para la creación de instancias de ViewModel.
 * Implementa la inyección de dependencias manual, proveyendo a los ViewModels
 * los repositorios, servicios y casos de uso necesarios para su funcionamiento.
 *
 * @property context Contexto de la aplicación utilizado para inicializar dependencias.
 */
class ViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Inicialización de componentes de infraestructura y datos.
        val apiClient = APIClient(context)
        val appDatabase = AppDatabase.getInstance(context)
        val samService = SamService(context)

        // Inicialización de implementaciones de repositorios.
        val authRepo = AuthRepositoryImpl(apiClient)
        val nfcRepo = NfcRepositoryImpl(context)

        // Orquestación y retorno de la instancia de ViewModel solicitada.
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                val loginUseCase = LoginUsuarioUseCase(authRepo)
                LoginViewModel(loginUseCase) as T
            }
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                val cortesHelper = ControlCortesHelper.getInstance(context)
                val realizarCobroUseCase = RealizarCobroUseCase(nfcRepo, appDatabase, cortesHelper)

                MainViewModel(
                    context = context,
                    realizarCobroUseCase = realizarCobroUseCase,
                    samService = samService,
                    cortesHelper = cortesHelper,
                    nfcRepository = nfcRepo,
                    db = appDatabase
                ) as T
            }
            else -> throw IllegalArgumentException("ViewModel no reconocido")
        }
    }
}
