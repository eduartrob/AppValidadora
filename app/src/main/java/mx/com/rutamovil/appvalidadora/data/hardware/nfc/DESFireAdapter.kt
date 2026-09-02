package mx.com.rutamovil.appvalidadora.data.hardware.nfc

import android.util.Log
import mx.com.rutamovil.appvalidadora.common.util.Utils
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.command.IsoDepWrapper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Arrays

/**
 * Adaptador que actúa como capa intermedia entre el motor de protocolo DESFire y la interfaz ISO-DEP del hardware.
 * Se encarga de encapsular los comandos en el formato nativo de DESFire (Native Wrapping) y gestionar
 * el encadenamiento de tramas (Chaining) para transferencias de datos que superan el MTU del dispositivo.
 *
 * @property isoDep Envoltorio de la interfaz ISO-DEP para comunicación NFC.
 * @property print Indica si se deben imprimir los tramas transmitidos y recibidos en el log.
 */
class DESFireAdapter(private val isoDep: IsoDepWrapper, private val print: Boolean) {

    companion object {
        private val TAG = DESFireAdapter::class.java.name
        /** Código de operación exitosa (OK). */
        const val OPERATION_OK = 0x00.toByte()
        /** Indicador de que existen tramas adicionales pendientes de lectura/envío. */
        const val ADDITIONAL_FRAME = 0xAF.toByte()
        /** Byte de estado primario (SW1) esperado para comandos encapsulados. */
        const val STATUS_OK = 0x91.toByte()
        /** Tamaño máximo permitido para una unidad de datos del comando (CAPDU). */
        const val MAX_CAPDU_SIZE = 55
        /** Tamaño máximo esperado para una respuesta (RAPDU). */
        const val MAX_RAPDU_SIZE = 60

        /**
         * Envuelve un comando simple sin parámetros en una estructura APDU compatible con DESFire.
         *
         * @param command Código del comando DESFire.
         * @return Arreglo de bytes representando la APDU completa.
         */
        @Throws(Exception::class)
        fun wrapMessage(command: Byte): ByteArray {
            return byteArrayOf(0x90.toByte(), command, 0x00, 0x00, 0x00)
        }

        /**
         * Envuelve un comando con parámetros de datos en una estructura APDU.
         *
         * @param command Código del comando DESFire.
         * @param parameters Cuerpo de datos o parámetros del comando.
         * @param offset Posición inicial de los datos en el arreglo.
         * @param length Cantidad de bytes de datos a incluir.
         * @return Arreglo de bytes representando la APDU completa.
         */
        @Throws(Exception::class)
        fun wrapMessage(command: Byte, parameters: ByteArray?, offset: Int, length: Int): ByteArray {
            val stream = ByteArrayOutputStream()
            stream.write(0x90)
            stream.write(command.toInt())
            stream.write(0x00)
            stream.write(0x00)
            if (parameters != null && length > 0) {
                stream.write(length)
                stream.write(parameters, offset, length)
            }
            stream.write(0x00)
            return stream.toByteArray()
        }

        /**
         * Genera una cadena hexadecimal a partir de un arreglo de bytes.
         */
        fun getHexString(a: ByteArray, space: Boolean): String {
            val sb = StringBuilder()
            for (b in a) {
                sb.append(String.format("%02x", b.toInt() and 0xff))
                if (space) sb.append(' ')
            }
            return sb.toString().trim().uppercase()
        }
    }

    private var debug = true

    /** Recupera el envoltorio ISO-DEP asociado. */
    fun getIsoDep(): IsoDepWrapper = isoDep

    /**
     * Transmite un comando y gestiona automáticamente el encadenamiento de entrada y salida.
     * Es el método preferido para lecturas y escrituras de archivos grandes.
     *
     * @param adpu APDU original a transmitir.
     * @return Respuesta consolidada tras procesar todos los tramas del encadenamiento.
     */
    @Throws(Exception::class)
    fun transmitChain(adpu: ByteArray): ByteArray {
        return receieveResponseChain(sendRequestChain(adpu))
    }

    /**
     * Gestiona la recepción de múltiples tramas de respuesta cuando la tarjeta indica ADDITIONAL_FRAME (0xAF).
     * Concatena los cuerpos de datos hasta recibir el estado de finalización OPERATION_OK (0x00).
     */
    @Throws(IOException::class, Exception::class)
    fun receieveResponseChain(response: ByteArray): ByteArray {
        var currentResponse = response
        if (debug) Log.d(TAG, "response: " + Utils.bytesToHex(currentResponse))

        if (currentResponse[currentResponse.size - 2] == STATUS_OK && currentResponse[currentResponse.size - 1] == OPERATION_OK) {
            return currentResponse
        }

        val output = ByteArrayOutputStream()
        while (true) {
            if (currentResponse[currentResponse.size - 2] != STATUS_OK) {
                throw Exception("Invalid response " + String.format("%02x", currentResponse[currentResponse.size - 2].toInt() and 0xff))
            }
            output.write(currentResponse, 0, currentResponse.size - 2)
            val status = currentResponse[currentResponse.size - 1]
            if (status == OPERATION_OK) {
                output.write(currentResponse, currentResponse.size - 2, 2)
                return output.toByteArray()
            } else if (status != ADDITIONAL_FRAME) {
                throw Exception("PICC error code while reading response: " + Integer.toHexString(status.toInt() and 0xFF))
            }
            currentResponse = transmit(wrapMessage(ADDITIONAL_FRAME))
        }
    }

    /**
     * Gestiona el envío fragmentado de datos cuando el comando supera el tamaño máximo de APDU permitido.
     * Envía tramas sucesivas utilizando el estado ADDITIONAL_FRAME hasta agotar los datos.
     */
    @Throws(Exception::class)
    fun sendRequestChain(apdu: ByteArray): ByteArray {
        var currentApdu = apdu
        if (currentApdu.size <= MAX_CAPDU_SIZE) {
            return transmit(currentApdu)
        }
        var offset = 5
        val nextCommand = currentApdu[1]
        if (debug) Log.d(TAG, "sendRequestChain with apdu.length >= MAX_CAPDU_SIZE")
        
        currentApdu = Arrays.copyOf(currentApdu, currentApdu.size - 1)
        
        while (true) {
            val nextLength = Math.min(MAX_CAPDU_SIZE - 1, currentApdu.size - offset)
            val request = wrapMessage(nextCommand, currentApdu, offset, nextLength)
            val response = transmit(request)
            
            if (response[response.size - 2] != STATUS_OK) {
                throw Exception("Invalid response " + String.format("%02x", response[response.size - 2].toInt() and 0xff))
            }

            offset += nextLength
            if (offset == currentApdu.size) {
                return response
            }

            if (response.size != 2) {
                throw IllegalArgumentException("Expected empty response payload while transmitting request")
            }
            val status = response[response.size - 1]
            if (status != ADDITIONAL_FRAME) {
                throw Exception("PICC error code: " + Integer.toHexString(status.toInt() and 0xFF))
            }
        }
    }

    /**
     * Transmisión básica de un arreglo de bytes a través de la interfaz NFC.
     *
     * @param command Arreglo de bytes (APDU) a enviar.
     * @return Respuesta cruda recibida de la tarjeta.
     */
    @Throws(IOException::class)
    fun transmit(command: ByteArray): ByteArray {
        if (print) {
            Log.d(TAG, "===> " + getHexString(command, true) + " (" + command.size + ")")
        }
        val response = isoDep.transceive(command)
        if (print) {
            Log.d(TAG, "<=== " + getHexString(response, true) + " (" + response.size + ")")
        }
        return response
    }
}
