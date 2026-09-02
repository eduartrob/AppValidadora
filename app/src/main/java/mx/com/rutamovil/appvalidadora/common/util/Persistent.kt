package mx.com.rutamovil.appvalidadora.common.util

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Interfaz que define las capacidades de persistencia binaria para los objetos del dominio.
 * Permite la serialización y deserialización manual de datos a través de flujos de datos.
 */
interface Persistent {
    /**
     * Lee el estado del objeto desde un flujo de entrada de datos.
     *
     * @param input Flujo de entrada de datos binarios.
     * @throws IOException Si ocurre un error durante la lectura.
     */
    @Throws(IOException::class)
    fun read(input: DataInputStream)

    /**
     * Escribe el estado actual del objeto en un flujo de salida de datos.
     *
     * @param output Flujo de salida para datos binarios.
     * @throws IOException Si ocurre un error durante la escritura.
     */
    @Throws(IOException::class)
    fun write(output: DataOutputStream)
}
