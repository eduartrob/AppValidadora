package mx.com.rutamovil.appvalidadora.data.hardware.nfc.command

import android.nfc.tech.IsoDep
import java.io.IOException

/**
 * Envoltorio predeterminado para la tecnología IsoDep.
 * Proporciona una implementación concreta de [IsoDepWrapper] delegando las operaciones al hardware NFC.
 *
 * @property isoDep Objeto nativo IsoDep de Android.
 */
class DefaultIsoDepWrapper(private val isoDep: IsoDep) : IsoDepWrapper {

    /**
     * Delega la transmisión de datos a la instancia nativa de IsoDep.
     *
     * @param data Datos a enviar.
     * @return Respuesta recibida.
     */
    @Throws(IOException::class)
    override fun transceive(data: ByteArray): ByteArray {
        return isoDep.transceive(data)
    }
}
