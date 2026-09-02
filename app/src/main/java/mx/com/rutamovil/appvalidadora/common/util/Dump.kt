package mx.com.rutamovil.appvalidadora.common.util

/**
 * Objeto de utilidad para la generación de representaciones textuales de datos binarios.
 * Proporciona métodos para convertir bytes y arreglos de bytes a cadenas en formato hexadecimal.
 */
object Dump {
    /**
     * Convierte un solo byte a su representación hexadecimal de dos caracteres.
     *
     * @param b Byte a convertir.
     * @return Cadena de texto con el valor hexadecimal.
     */
    fun hex(b: Byte): String {
        return String.format("%02x", b.toInt() and 0xFF)
    }

    /**
     * Convierte un arreglo de bytes a una cadena hexadecimal, con espacios entre cada byte por defecto.
     *
     * @param a Arreglo de bytes a convertir.
     * @return Cadena de texto hexadecimal resultante.
     */
    fun hex(a: ByteArray?): String {
        return hex(a, true)
    }

    /**
     * Convierte un arreglo de bytes a una cadena hexadecimal permitiendo especificar si se incluyen espacios.
     *
     * @param a Arreglo de bytes a convertir.
     * @param space Indica si se debe agregar un espacio entre cada par de caracteres hexadecimales.
     * @return Cadena de texto hexadecimal resultante o una cadena vacía si el arreglo es nulo.
     */
    fun hex(a: ByteArray?, space: Boolean): String {
        if (a == null) return ""
        val sb = StringBuilder()
        if (space) {
            for (b in a) {
                sb.append(hex(b)).append(' ')
            }
            if (sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1)
            }
        } else {
            for (b in a) {
                sb.append(hex(b))
            }
        }
        return sb.toString()
    }
}
