package mx.com.rutamovil.appvalidadora.data.hardware.nfc.command

import java.io.IOException

/**
 * Interfaz que define las operaciones de bajo nivel para la comunicación con tarjetas inteligentes sin contacto
 * utilizando el protocolo ISO-DEP (ISO 14443-4).
 */
interface IsoDepAdapter {
    /**
     * Envía una unidad de datos de protocolo (APDU) y espera la respuesta correspondiente.
     *
     * @param data Datos binarios a transmitir.
     * @return Respuesta recibida de la tarjeta inteligente.
     * @throws IOException Si ocurre un error de comunicación o pérdida de conexión.
     */
    @Throws(IOException::class)
    fun transceive(data: ByteArray): ByteArray
}
