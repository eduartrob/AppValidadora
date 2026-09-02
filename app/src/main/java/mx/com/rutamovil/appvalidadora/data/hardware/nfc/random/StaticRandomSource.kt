package mx.com.rutamovil.appvalidadora.data.hardware.nfc.random

/**
 * Fuente de aleatorios estática (predecible) utilizada principalmente para pruebas unitarias
 * y depuración de protocolos criptográficos con vectores de prueba conocidos.
 *
 * @property bytes Conjunto de bytes fijos que se devolverán en las peticiones.
 */
class StaticRandomSource(private val bytes: ByteArray) : RandomSource {

    override fun getRandom(length: Int): ByteArray {
        val random = ByteArray(length)
        fillRandom(random)
        return random
    }

    override fun fillRandom(bytes: ByteArray) {
        System.arraycopy(this.bytes, 0, bytes, 0, Math.min(this.bytes.size, bytes.size))
    }
}
