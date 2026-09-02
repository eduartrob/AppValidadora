package mx.com.rutamovil.appvalidadora.common.util

import android.content.Context
import java.util.UUID

/**
 * Gestor encargado de la administración de claves de cifrado para la seguridad local.
 * Proporciona mecanismos para la persistencia y recuperación de frases de seguridad.
 */
object EncryptionManager {
    /** Nombre del archivo de preferencias compartidas donde se almacenan las claves. */
    private const val PREFS_NAME = "encryption_prefs"
    
    /** Clave de identificación para la frase de seguridad de la base de datos. */
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    /**
     * Obtiene la clave de cifrado para la base de datos o genera una nueva si no existe previamente.
     * La clave generada se persiste en el almacenamiento privado de la aplicación.
     *
     * @param context Contexto de la aplicación para acceder al almacenamiento.
     * @return Frase de seguridad en formato cadena de texto.
     */
    fun getOrCreateKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var key = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (key == null) {
            key = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DB_PASSPHRASE, key).apply()
        }
        return key
    }
}
