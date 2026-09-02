package mx.com.rutamovil.appvalidadora.data.hardware.nfc.random

import java.security.SecureRandom

/**
 * Implementación predeterminada de generación de aleatorios utilizando [SecureRandom].
 * Proporciona aleatoriedad criptográficamente fuerte para uso en producción.
 */
class DefaultRandomSource : RandomSource {
    private val random = SecureRandom()

    override fun getRandom(length: Int): ByteArray {
        val bytes = ByteArray(length)
        fillRandom(bytes)
        return bytes
    }

    override fun fillRandom(bytes: ByteArray) {
        random.nextBytes(bytes)
    }
}
