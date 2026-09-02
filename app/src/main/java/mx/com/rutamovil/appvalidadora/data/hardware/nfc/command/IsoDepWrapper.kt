package mx.com.rutamovil.appvalidadora.data.hardware.nfc.command

import java.io.IOException

/**
 * Interfaz simplificada para la transmisión de datos a través de ISO-DEP.
 * Actúa como una abstracción sobre los mecanismos de comunicación NFC del sistema.
 */
interface IsoDepWrapper {
    /**
     * Realiza el intercambio de datos (transmisión y recepción) con la tarjeta inteligente.
     *
     * @param data Comandos binarios a enviar.
     * @return Datos binarios devueltos por la tarjeta.
     * @throws IOException Ante fallos en la capa de transporte NFC.
     */
    @Throws(IOException::class)
    fun transceive(data: ByteArray): ByteArray
}
