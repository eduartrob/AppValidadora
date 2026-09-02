package mx.com.rutamovil.appvalidadora.data.hardware.nfc.crypto

/**
 * Objeto para el cálculo de la suma de comprobación de redundancia cíclica de 16 bits (CRC16).
 * Sigue el estándar comúnmente utilizado en protocolos de comunicación de tarjetas inteligentes (ISO/IEC 14443-3).
 */
object CRC16 {
    /**
     * Calcula el CRC16 para la totalidad de un arreglo de bytes.
     *
     * @param a Datos de entrada.
     * @return Arreglo de 2 bytes con el valor del CRC16 calculado.
     */
    fun get(a: ByteArray): ByteArray {
        return get(a, 0, a.size)
    }

    /**
     * Calcula el CRC16 para una sección específica de un arreglo de bytes.
     *
     * @param a Arreglo de origen.
     * @param offset Posición inicial de los datos.
     * @param length Cantidad de bytes a procesar.
     * @return Arreglo de 2 bytes con el CRC16 resultante.
     */
    fun get(a: ByteArray, offset: Int, length: Int): ByteArray {
        val crc = crcA(a, offset, length)
        val ret = ByteArray(2)
        ret[1] = (crc shr 8 and 0xff).toByte()
        ret[0] = (crc and 0xff).toByte()
        return ret
    }

    /** Implementación polinómica del cálculo CRC-A. */
    private fun crcA(a: ByteArray, offset: Int, length: Int): Int {
        var crc = 0x6363
        for (i in offset until offset + length) {
            crc = addByte(crc, a[i])
        }
        return crc
    }

    /** Procesa un solo byte e integra su valor en el acumulador CRC. */
    private fun addByte(crc: Int, b: Byte): Int {
        var bb = (b.toInt() xor crc) and 0xFF
        bb = (bb xor (bb shl 4)) and 0xFF
        return (crc shr 8) xor (bb shl 8) xor (bb shl 3) xor (bb shr 4)
    }
}
