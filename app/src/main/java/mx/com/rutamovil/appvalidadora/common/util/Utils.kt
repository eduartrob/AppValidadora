package mx.com.rutamovil.appvalidadora.common.util

import android.os.Build
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Colección de funciones de utilidad general para manipulación de datos, conversiones y gestión de tiempo.
 */
object Utils {

    /**
     * Convierte un arreglo de bytes a una cadena en representación hexadecimal.
     *
     * @param bytes Arreglo de bytes a procesar.
     * @return Cadena de texto hexadecimal.
     */
    fun bytesToHex(bytes: ByteArray?): String {
        return bytesToHex(bytes, false)
    }

    /**
     * Convierte un arreglo de bytes a una cadena hexadecimal permitiendo la inclusión de espacios separadores.
     *
     * @param bytes Arreglo de bytes a procesar.
     * @param space Verdadero si se desea un espacio entre cada byte.
     * @return Cadena de texto hexadecimal resultante.
     */
    fun bytesToHex(bytes: ByteArray?, space: Boolean): String {
        if (bytes == null) return ""
        val result = StringBuilder()
        for (b in bytes) {
            result.append(String.format("%02X", b.toInt() and 0xFF))
            if (space) result.append(" ")
        }
        return result.toString().trim()
    }

    /**
     * Convierte una sección de un arreglo de bytes a un valor de tipo entero.
     *
     * @param b Arreglo de bytes de origen.
     * @param offset Posición inicial en el arreglo.
     * @param length Cantidad de bytes a convertir.
     * @return Valor entero obtenido.
     */
    fun byteArrayToInt(b: ByteArray, offset: Int = 0, length: Int = b.size): Int {
        return byteArrayToLong(b, offset, length).toInt()
    }

    /**
     * Convierte una secuencia de bytes a un valor de tipo Long, siguiendo el orden de red (Big-Endian).
     *
     * @param b Arreglo de bytes de origen.
     * @param offset Posición inicial para la conversión.
     * @param length Número de bytes a procesar.
     * @return Valor de tipo Long resultante.
     */
    fun byteArrayToLong(b: ByteArray, offset: Int = 0, length: Int = b.size): Long {
        var value: Long = 0
        for (i in 0 until length) {
            val shift = (length - 1 - i) * 8
            value += (b[i + offset].toLong() and 0xFFL) shl shift
        }
        return value
    }

    /**
     * Transforma una cadena de texto hexadecimal en su arreglo de bytes correspondiente.
     *
     * @param s Cadena de texto hexadecimal.
     * @return Arreglo de bytes o nulo si la cadena no tiene un formato válido.
     */
    fun hexStringToByteArray(s: String?): ByteArray? {
        if (s == null) return null
        return try {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) +
                        Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            data
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Elimina todos los caracteres que no sean alfanuméricos de una cadena de texto.
     *
     * @param s Cadena de texto original.
     * @return Cadena procesada conteniendo únicamente letras y números.
     */
    fun removeAllNonAlphaNumeric(s: String?): String? {
        return s?.replace("[^A-Za-z0-9]".toRegex(), "")
    }

    /**
     * Genera una estampa de tiempo actual formateada como "yyyy.MM.dd HH:mm:ss".
     * Soporta compatibilidad entre versiones de Android inferiores y superiores a Oreo.
     *
     * @return Cadena de texto con la fecha y hora actual.
     */
    fun getTimestamp(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ZonedDateTime.now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
        } else {
            SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US).format(Date())
        }
    }

    /**
     * Convierte valores de DP a pixeles según la densidad de la pantalla.
     */
    fun dpToPx(context: android.content.Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }
}
