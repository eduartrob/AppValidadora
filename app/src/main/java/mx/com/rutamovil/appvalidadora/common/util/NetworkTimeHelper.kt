package mx.com.rutamovil.appvalidadora.common.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

/**
 * Asistente para la gestión y validación de la sincronización de la hora a través de la red.
 * Asegura que el dispositivo tenga configurada la hora automática para garantizar la integridad de los registros.
 *
 * @property context Contexto de la aplicación necesario para consultar configuraciones del sistema y mostrar diálogos.
 */
class NetworkTimeHelper(private val context: Context) {

    /**
     * Verifica si la opción de hora automática (proporcionada por la red) está habilitada en el sistema.
     *
     * @return Verdadero si la hora automática está habilitada, falso en caso contrario.
     */
    fun isNetworkTimeEnabled(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene la estampa de tiempo actual del sistema si la hora de red está habilitada.
     *
     * @return El tiempo actual en milisegundos si la sincronización de red está activa, de lo contrario devuelve 0.
     */
    fun getNetworkTime(): Long {
        return if (isNetworkTimeEnabled()) System.currentTimeMillis() else 0L
    }

    /**
     * Muestra un cuadro de diálogo informativo instando al usuario a habilitar la hora automática de la red.
     * Proporciona un acceso directo a la configuración de fecha y hora del sistema.
     *
     * @param onContinue Acción opcional a ejecutar si el usuario decide ignorar la recomendación.
     */
    fun showEnableNetworkTimeDialog(onContinue: Runnable?) {
        AlertDialog.Builder(context)
            .setTitle("⚠️ HORA NO CONFIGURADA")
            .setMessage("Para que el validador funcione correctamente,\n\n" +
                    "ACTIVAR:\n\n" +
                    "📱 Configuración → Fecha y hora →\n" +
                    "   USAR HORA DE LA RED\n\n" +
                    "Esto garantiza la hora correcta incluso sin internet.")
            .setPositiveButton("Abrir Configuración") { _, _ ->
                context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
            }
            .setNegativeButton("Ignorar") { _, _ ->
                onContinue?.run()
            }
            .show()
    }
}
