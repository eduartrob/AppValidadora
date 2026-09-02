package mx.com.rutamovil.appvalidadora.common.util

/**
 * Objeto de utilidad para realizar operaciones a nivel de bits y conversiones de datos numéricos.
 * Se especializa en la conversión entre enteros y arreglos de bytes siguiendo el orden Little-Endian (LSB).
 */
object BitOp {
    /**
     * Convierte un valor entero a un arreglo de 4 bytes en formato Little-Endian.
     *
     * @param value Valor entero a convertir.
     * @return Arreglo de bytes resultante.
     */
    fun intToLsb(value: Int): ByteArray {
        val a = ByteArray(4)
        var v = value
        for (i in 0 until 4) {
            a[i] = (v and 0xFF).toByte()
            v = v ushr 8
        }
        return a
    }

    /**
     * Convierte un valor entero a formato Little-Endian y lo almacena en un arreglo de bytes existente.
     *
     * @param value Valor entero a convertir.
     * @param a Arreglo de bytes donde se almacenará el resultado.
     * @param offset Posición inicial en el arreglo para escribir los datos.
     */
    fun intToLsb(value: Int, a: ByteArray, offset: Int) {
        var v = value
        for (i in offset until (offset + 4)) {
            a[i] = (v and 0xFF).toByte()
            v = v ushr 8
        }
    }

    /**
     * Convierte una secuencia de 4 bytes desde un arreglo a un valor entero, asumiendo formato Little-Endian.
     *
     * @param a Arreglo de bytes de origen.
     * @param offset Posición inicial en el arreglo donde comienza el valor de 4 bytes.
     * @return Valor entero resultante de la conversión.
     */
    fun lsbToInt(a: ByteArray, offset: Int): Int {
        var ret = 0
        ret = ret or ((a[3 + offset].toInt() and 0xff) shl 24)
        ret = ret or ((a[2 + offset].toInt() and 0xff) shl 16)
        ret = ret or ((a[1 + offset].toInt() and 0xff) shl 8)
        ret = ret or ((a[0 + offset].toInt() and 0xff) shl 0)
        return ret
    }

    /**
     * Variante de conversión de arreglo de bytes (4 bytes) a entero en formato Little-Endian.
     *
     * @param a Arreglo de bytes que contiene el valor.
     * @param offset Posición inicial del valor en el arreglo.
     * @return Valor entero convertido.
     */
    fun lsbToInt4(a: ByteArray, offset: Int): Int {
        var ret = 0
        ret = ret or ((a[3 + offset].toInt() and 0xff) shl 24)
        ret = ret or ((a[2 + offset].toInt() and 0xff) shl 16)
        ret = ret or ((a[1 + offset].toInt() and 0xff) shl 8)
        ret = ret or ((a[0 + offset].toInt() and 0xff) shl 0)
        return ret
    }
}
