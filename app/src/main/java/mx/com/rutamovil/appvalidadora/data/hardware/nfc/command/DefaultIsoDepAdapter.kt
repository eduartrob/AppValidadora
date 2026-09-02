package mx.com.rutamovil.appvalidadora.data.hardware.nfc.command

import android.nfc.tech.IsoDep
import android.util.Log
import mx.com.rutamovil.appvalidadora.common.util.Dump
import java.io.IOException

/**
 * Implementación estándar del adaptador ISO-DEP que utiliza la API nativa de Android.
 * Incluye lógica de registro (logging) para depuración de la comunicación a nivel de bytes.
 *
 * @property isoDep Instancia nativa de la tecnología IsoDep detectada por el sistema.
 */
class DefaultIsoDepAdapter(private val isoDep: IsoDep) : IsoDepAdapter {

    companion object {
        private val TAG = DefaultIsoDepAdapter::class.java.name
    }

    /**
     * Transmite los datos binarios utilizando el método transceive del hardware NFC.
     * Registra en el Logcat los tramas enviados y recibidos en formato hexadecimal.
     *
     * @param data APDU de comando.
     * @return APDU de respuesta.
     */
    @Throws(IOException::class)
    override fun transceive(data: ByteArray): ByteArray {
        Log.d(TAG, "===> " + Dump.hex(data))
        val response = isoDep.transceive(data)
        Log.d(TAG, "<=== " + Dump.hex(response))
        return response
    }
}
