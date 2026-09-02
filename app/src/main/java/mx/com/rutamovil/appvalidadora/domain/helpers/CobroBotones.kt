package mx.com.rutamovil.appvalidadora.domain.helpers

import android.os.Handler
import android.os.Looper

/**
 * Gestor de eventos para los botones físicos de cobro del dispositivo validador.
 * Administra el estado de selección de tarifas, los tiempos de espera (timeout)
 * y la comunicación con la interfaz de usuario.
 *
 * @property callback Interfaz de retorno para notificar cambios de estado a la vista.
 */
class CobroBotones(private val callback: CobroCallback) {

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    
    /** Perfil de tarifa actualmente seleccionado y en espera de una tarjeta. */
    var categoriaPendiente: String? = null
        private set
        
    /** Determina si se aplicará la tarifa mínima del perfil seleccionado. */
    var esMinimoPendiente: Boolean = false
        private set

    /** Interfaz para la notificación de eventos de selección de botones. */
    interface CobroCallback {
        /** Invocado cuando el sistema entra en modo de espera para detectar una tarjeta. */
        fun onEsperandoTarjeta(mensaje: String)
        /** Invocado cuando el tiempo de espera de selección expira o se cancela. */
        fun onResetTarifa()
    }

    /**
     * Procesa la pulsación de una tecla física y activa el perfil de cobro correspondiente.
     *
     * @param keyCode Código de la tecla presionada.
     * @return Verdadero si la tecla corresponde a una función de cobro válida.
     */
    fun procesarTecla(keyCode: Int): Boolean {
        val (categoria, esMinimo) = when (keyCode) {
            192 -> "REGULAR" to true
            191 -> "REGULAR" to false
            194 -> "ESTUDIANTE" to true
            190 -> "ESTUDIANTE" to false
            193 -> "3ERA EDAD" to true
            189 -> "3ERA EDAD" to false
            188 -> "DISCAPACITADO" to true
            else -> return false
        }

        this.categoriaPendiente = categoria
        this.esMinimoPendiente = esMinimo

        iniciarTimeout()
        val modoTexto = if (esMinimo) "MÍNIMA" else "MÁXIMA"
        callback.onEsperandoTarjeta("$categoria ($modoTexto) - ACERQUE TARJETA")
        return true
    }

    /** Indica si existe una tarifa seleccionada esperando por una tarjeta. */
    fun hayCobroPendiente(): Boolean = categoriaPendiente != null

    /** Restablece el estado de selección y detiene los temporizadores activos. */
    fun limpiarSeleccion() {
        cancelarTimeout()
        categoriaPendiente = null
    }

    /**
     * Inicia un temporizador de 30 segundos. Si no se detecta una tarjeta en este lapso,
     * se cancela la selección de tarifa por seguridad y conveniencia del usuario.
     */
    private fun iniciarTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }

        timeoutRunnable = Runnable {
            callback.onResetTarifa()
        }
        
        handler.postDelayed(timeoutRunnable!!, 30000)
    }

    /** Detiene el temporizador de espera de tarjeta. */
    fun cancelarTimeout() {
        timeoutRunnable?.let { 
            handler.removeCallbacks(it)
            timeoutRunnable = null
        }
    }
}
