package mx.com.rutamovil.appvalidadora.data.hardware.nfc.crypto

import java.util.zip.CRC32 as ZipCRC32

import kotlin.experimental.inv

/**
 * Objeto encargado del cálculo de la suma de comprobación de redundancia cíclica de 32 bits (CRC32).
 * Utilizado principalmente en protocolos modernos de comunicación con tarjetas DESFire para validación de tramas.
 */
object CRC32 {
    /**
     * Calcula el valor CRC32 para un arreglo de bytes completo.
     *
     * @param a Arreglo de bytes de entrada.
     * @return Arreglo de 4 bytes con el CRC32 calculado.
     */
    fun get(a: ByteArray): ByteArray {
        return get(a, 0, a.size)
    }

    /**
     * Calcula el CRC32 para una porción de datos, integrando la inversión de bits requerida por ciertos protocolos.
     *
     * @param a Arreglo de bytes de origen.
     * @param offset Posición inicial.
     * @param length Longitud de los datos.
     * @return Arreglo de 4 bytes con el valor de comprobación.
     */
    fun get(a: ByteArray, offset: Int, length: Int): ByteArray {
        val crc = ZipCRC32()
        crc.update(a, offset, length)
        var l = crc.value

        val ret = ByteArray(4)
        for (i in 0 until 4) {
            ret[i] = (l and 0xFFL).toByte().inv()
            l = l ushr 8
        }
        return ret
    }
}
