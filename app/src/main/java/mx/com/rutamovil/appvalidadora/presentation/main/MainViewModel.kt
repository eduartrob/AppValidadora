package mx.com.rutamovil.appvalidadora.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.com.rutamovil.appvalidadora.common.Constants
import mx.com.rutamovil.appvalidadora.common.util.Utils
import mx.com.rutamovil.appvalidadora.data.hardware.sam.SamService
import mx.com.rutamovil.appvalidadora.data.sync.SyncWorker
import mx.com.rutamovil.appvalidadora.domain.helpers.ControlCortesHelper
import mx.com.rutamovil.appvalidadora.domain.helpers.TarifaControl
import mx.com.rutamovil.appvalidadora.domain.usecases.RealizarCobroUseCase
import android.content.Context
import android.util.Log
import mx.com.rutamovil.appvalidadora.data.local.AppDatabase
import mx.com.rutamovil.appvalidadora.data.remote.ApiService
import mx.com.rutamovil.appvalidadora.data.remote.models.CardsResponse
import mx.com.rutamovil.appvalidadora.data.remote.models.Fare
import mx.com.rutamovil.appvalidadora.domain.repositories.INfcRepository

/**
 * Datos necesarios para mostrar el diálogo de selección de tarifa.
 */
data class FareSelectionData(
    val tag: Any,
    val uidHex: String,
    val roleName: String,
    val fares: List<Fare>
)

/**
 * Representación de los estados operativos del validador.
 */
sealed class ValidadorState {
    /** El sistema está listo para detectar una tarjeta. */
    object Listo : ValidadorState()
    /** Se está ejecutando una transacción sobre el hardware. */
    object Procesando : ValidadorState()
    /** La transacción fue exitosa. @property saldoRestante Nuevo saldo de la tarjeta. */
    data class Exito(val saldoRestante: String) : ValidadorState()
    /** Fallo en la transacción. @property mensaje Causa del error. */
    data class Error(val mensaje: String) : ValidadorState()
}

/**
 * ViewModel central de la aplicación.
 * Orquesta la derivación de llaves, el flujo de cobro NFC, la actualización del servidor 
 * y la gestión de tarifas dinámicas.
 *
 * @property context Contexto para gestión de WorkManager.
 * @property realizarCobroUseCase Lógica central de negocio para el cobro físico.
 * @property samService Servicio de derivación de llaves criptográficas.
 * @property cortesHelper Acceso al catálogo de tarifas local.
 */
class MainViewModel(
    private val context: Context,
    private val realizarCobroUseCase: RealizarCobroUseCase,
    private val samService: SamService,
    private val cortesHelper: ControlCortesHelper,
    private val nfcRepository: INfcRepository,
    private val db: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ValidadorState>(ValidadorState.Listo)
    /** Estado reactivo de la interfaz principal. */
    val uiState: StateFlow<ValidadorState> = _uiState.asStateFlow()

    private val _showFareSelection = MutableStateFlow<FareSelectionData?>(null)
    /** Evento para mostrar el diálogo de selección de tarifa manual. */
    val showFareSelection: StateFlow<FareSelectionData?> = _showFareSelection.asStateFlow()

    private val _tarifaActual = MutableStateFlow(0.0)
    /** Valor monetario de la tarifa seleccionada. */
    val tarifaActual: StateFlow<Double> = _tarifaActual.asStateFlow()

    private val _tarifaNombre = MutableStateFlow("")
    /** Nombre descriptivo de la tarifa seleccionada. */
    val tarifaNombre: StateFlow<String> = _tarifaNombre.asStateFlow()

    private val tarifaControl = TarifaControl()

    init {
        cargarTarifasDesdeBD()
    }

    /** Carga el catálogo de tarifas desde el caché local al controlador de niveles. */
    fun cargarTarifasDesdeBD() {
        viewModelScope.launch {
            val tarifas = cortesHelper.obtenerTarifasCache()
            if (tarifas.isNotEmpty()) {
                tarifaControl.limpiar()
                for (t in tarifas) {
                    val precio = t.price?.toDoubleOrNull() ?: 0.0
                    tarifaControl.agregarNivel(t.id, precio, t.fare ?: "SIN NOMBRE")
                }
                actualizarInfoTarifa()
            }
        }
    }

    /** Permite navegar entre los niveles de tarifa disponibles. */
    fun cambiarTarifa(subir: Boolean) {
        if (subir) tarifaControl.subirNivel() else tarifaControl.bajarNivel()
        actualizarInfoTarifa()
    }

    /** Sincroniza los flujos de estado con la selección actual del controlador de tarifas. */
    private fun actualizarInfoTarifa() {
        _tarifaActual.value = tarifaControl.getPrecioActual()
        val nivel = tarifaControl.getListaNiveles().getOrNull(
            tarifaControl.getListaNiveles().indexOfFirst { it.precio == _tarifaActual.value }
        )
        _tarifaNombre.value = nivel?.nombreBackend ?: "DESCONOCIDA"
    }

    /**
     * Inicia el proceso de cobro tras la detección de una tarjeta.
     * Realiza la derivación de la llave administrativa única para la tarjeta 
     * antes de invocar el caso de uso de cobro.
     *
     * @param tag Etiqueta NFC detectada.
     * @param uidBytes UID en bytes.
     * @param uidHex UID en hexadecimal.
     * @param categoriaBoton Perfil de usuario seleccionado (opcional).
     * @param esMinimo Indica si se aplica tarifa mínima.
     */
    fun procesarCobroNfc(
        tag: Any,
        uidBytes: ByteArray,
        uidHex: String,
        categoriaBoton: String?,
        esMinimo: Boolean
    ) {
        if (_uiState.value is ValidadorState.Procesando) return
        
        // Si no hay categoría seleccionada, detectamos el rol y mostramos el menú.
        if (categoriaBoton == null) {
            detectarYMostrarMenuTarifas(tag, uidBytes, uidHex)
            return
        }

        _uiState.value = ValidadorState.Procesando

        viewModelScope.launch {
            try {
                // 1. Derivación de la llave maestra de transporte única para este UID.
                val derivedKeyBytes = samService.deriveKey(uidBytes, "TRANSPORT_MASTER_KEY")
                val adminKeyHex = Utils.bytesToHex(derivedKeyBytes)

                // 2. Ejecución de la lógica integral de cobro.
                val (exito, resultado, montoCobrado) = realizarCobroUseCase(
                    tag = tag,
                    uidHex = uidHex,
                    aid = Constants.ID_BASE,
                    adminKeyHex = adminKeyHex,
                    categoriaBoton = categoriaBoton,
                    esMinimo = esMinimo
                )

                if (exito) {
                    _uiState.value = ValidadorState.Exito(resultado)
                    
                    Log.d("COBRO_FLOW", "✅ Cobro físico exitoso. UID: $uidHex, Saldo chip: $resultado, Cobrado: $$montoCobrado")

                    // 3. Notificación inmediata al servidor para reflejar el nuevo saldo.
                    Log.d("COBRO_FLOW", "📡 Enviando actualización al servidor...")
                    ApiService.realizarCobro(uidHex, montoCobrado, object : ApiService.ApiCallback<CardsResponse> {
                        override fun onSuccess(response: CardsResponse) {
                            val serverBalance = response.data?.balance ?: "N/A"
                            Log.d("COBRO_FLOW", "✨ SERVIDOR ACTUALIZADO CORRECTAMENTE")
                            Log.d("COBRO_FLOW", "📊 Saldo en servidor: $$serverBalance")
                        }
                        override fun onError(error: String) {
                            Log.e("COBRO_FLOW", "❌ ERROR AL ACTUALIZAR SERVIDOR: $error")
                        }
                    })

                    // 4. Disparo de sincronización forzada para asegurar que la transacción local llegue al backend.
                    dispararSincronizacion()
                } else {
                    Log.w("COBRO_FLOW", "❌ Cobro fallido: $resultado")
                    _uiState.value = ValidadorState.Error(resultado)
                }
            } catch (e: Exception) {
                _uiState.value = ValidadorState.Error(e.message ?: "Error de comunicación con el chip")
            }
        }
    }

    /** Regresa el validador al estado de espera inicial. */
    fun resetState() {
        _uiState.value = ValidadorState.Listo
        _showFareSelection.value = null
    }

    /** Descarta el diálogo de selección de tarifa. */
    fun cancelarSeleccionTarifa() {
        _showFareSelection.value = null
    }

    /**
     * Detecta el rol de la tarjeta y dispara el menú de selección de precios.
     */
    private fun detectarYMostrarMenuTarifas(tag: Any, uidBytes: ByteArray, uidHex: String) {
        viewModelScope.launch {
            try {
                // 1. Conexión y Autenticación básica para leer el rol.
                val derivedKeyBytes = samService.deriveKey(uidBytes, "TRANSPORT_MASTER_KEY")
                val adminKeyHex = Utils.bytesToHex(derivedKeyBytes)

                if (!nfcRepository.connect(tag)) return@launch
                if (!nfcRepository.authenticate(Constants.ID_BASE, adminKeyHex)) {
                    nfcRepository.disconnect()
                    _uiState.value = ValidadorState.Error("Tarjeta Inválida (Autenticación Fallida)")
                    return@launch
                }

                val rolCode = nfcRepository.readRole()
                nfcRepository.disconnect()

                val rolName = when (rolCode.toByte()) {
                    Constants.ROLE_REGULAR -> "REGULAR"
                    Constants.ROLE_ESTUDIANTE -> "ESTUDIANTE"
                    Constants.ROLE_TERCERA_EDAD -> "3ERA EDAD"
                    Constants.ROLE_PCD -> "DISCAPACITADO"
                    else -> "DESCONOCIDO"
                }

                // 2. Obtención de tarifas para el rol detectado.
                val fares = cortesHelper.getTarifasPorCategoria(rolName)
                if (fares.isEmpty()) {
                    _uiState.value = ValidadorState.Error("No hay precios cargados para $rolName")
                    return@launch
                }

                // 3. Disparo del evento UI.
                _showFareSelection.value = FareSelectionData(tag, uidHex, rolName, fares)

            } catch (e: Exception) {
                _uiState.value = ValidadorState.Error("Error al detectar tarjeta: ${e.message}")
            }
        }
    }

    /**
     * Ejecuta el cobro con un precio seleccionado manualmente desde el diálogo.
     */
    fun confirmarCobroManual(data: FareSelectionData, price: Double, fareName: String) {
        _showFareSelection.value = null
        _uiState.value = ValidadorState.Procesando
        _tarifaActual.value = price
        _tarifaNombre.value = fareName

        viewModelScope.launch {
            try {
                val derivedKeyBytes = samService.deriveKey(Utils.hexStringToByteArray(data.uidHex)!!, "TRANSPORT_MASTER_KEY")
                val adminKeyHex = Utils.bytesToHex(derivedKeyBytes)

                // En el cobro manual, pasamos la categoría detectada. 
                // El UseCase aplicará la tarifa que le pasemos indirectamente 
                // (tendremos que modificar el UseCase o inyectar el monto).
                
                // Opción A: Modificar RealizarCobroUseCase para aceptar un monto directo.
                // Opción B: Simular que se presionó el botón correspondiente.
                
                // Usaremos una versión simplificada del cobro aquí o modificaremos el UseCase.
                ejecutarCobroDirecto(data.tag, data.uidHex, data.roleName, price, adminKeyHex, fareName)

            } catch (e: Exception) {
                _uiState.value = ValidadorState.Error(e.message ?: "Error en cobro manual")
            }
        }
    }

    private suspend fun ejecutarCobroDirecto(
        tag: Any,
        uidHex: String,
        roleName: String,
        monto: Double,
        adminKeyHex: String,
        fareName: String
    ) {
        // Implementación directa que evita la validación de botón min/max del UseCase original
        try {
            if (!nfcRepository.connect(tag)) {
                _uiState.value = ValidadorState.Error("Fallo de conexión")
                return
            }
            if (!nfcRepository.authenticate(Constants.ID_BASE, adminKeyHex)) {
                _uiState.value = ValidadorState.Error("Autenticación fallida")
                return
            }

            val saldoActual = nfcRepository.readBalance()
            if (saldoActual < monto) {
                _uiState.value = ValidadorState.Error("SALDO INSUFICIENTE ($$saldoActual)")
                nfcRepository.disconnect()
                return
            }

            if (nfcRepository.debitBalance(monto)) {
                val nuevoSaldo = nfcRepository.readBalance()
                nfcRepository.disconnect()
                
                // Guardar transacción local
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                val transaction = mx.com.rutamovil.appvalidadora.data.local.entity.TransactionEntity(
                    cardUuid = uidHex,
                    passengerType = roleName,
                    amount = monto,
                    description = "Cobro Manual: $fareName",
                    chargedAt = sdf.format(java.util.Date()),
                    synced = false,
                    deviceUptimeMs = android.os.SystemClock.elapsedRealtime()
                )
                db.transactionDao().insert(transaction)
                
                _uiState.value = ValidadorState.Exito(nuevoSaldo.toString())
                
                // Update server
                ApiService.realizarCobro(uidHex, monto, object : ApiService.ApiCallback<CardsResponse> {
                    override fun onSuccess(response: CardsResponse) {}
                    override fun onError(error: String) {}
                })
                dispararSincronizacion()
            } else {
                _uiState.value = ValidadorState.Error("Error al descontar saldo")
                nfcRepository.disconnect()
            }
        } catch (e: Exception) {
            _uiState.value = ValidadorState.Error("Error: ${e.message}")
        }
    }

    /** Solicita a WorkManager la ejecución inmediata de una tarea de sincronización. */
    private fun dispararSincronizacion() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d("SYNC", "SyncWorker encolado tras cobro exitoso")
        } catch (e: Exception) {
            Log.e("SYNC", "Error al encolar SyncWorker: ${e.message}")
        }
    }
}
