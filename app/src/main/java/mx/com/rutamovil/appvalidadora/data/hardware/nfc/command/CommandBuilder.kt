package mx.com.rutamovil.appvalidadora.data.hardware.nfc.command

import java.io.ByteArrayOutputStream

/**
 * Asistente para la construcción dinámica de flujos de datos binarios (payloads) de comandos.
 * Facilita la concatenación de bytes y valores numéricos con diferentes formatos de ordenamiento.
 *
 * @param capacity Capacidad inicial del flujo de salida.
 */
class CommandBuilder(capacity: Int) {
    private val stream = ByteArrayOutputStream(capacity)

    /** Agrega un solo byte al flujo. */
    fun bytes1(b: Byte): CommandBuilder {
        stream.write(b.toInt())
        return this
    }

    /**
     * Agrega un valor entero de 3 bytes siguiendo el orden Little-Endian (LSB).
     * Utilizado comúnmente en DESFire para desplazamientos (offsets) y longitudes.
     *
     * @param v Valor entero a convertir.
     */
    fun bytes3Lsb(v: Int): CommandBuilder {
        stream.write(v and 0xFF)
        stream.write(v shr 8 and 0xFF)
        stream.write(v shr 16 and 0xFF)
        return this
    }

    /**
     * Obtiene el arreglo de bytes resultante de la construcción del comando.
     *
     * @return Arreglo de bytes consolidado.
     */
    fun bytes(): ByteArray = stream.toByteArray()
}
