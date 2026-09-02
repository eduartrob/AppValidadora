package mx.com.rutamovil.appvalidadora.data.hardware.nfc.random

/**
 * Interfaz que define la capacidad de generación de números aleatorios.
 * Crucial para la seguridad de los protocolos de autenticación mutua de tarjetas inteligentes.
 */
interface RandomSource {
    /** Genera un arreglo de bytes aleatorios de la longitud especificada. */
    fun getRandom(length: Int): ByteArray
    /** Llena un arreglo de bytes existente con valores aleatorios. */
    fun fillRandom(bytes: ByteArray)
}
