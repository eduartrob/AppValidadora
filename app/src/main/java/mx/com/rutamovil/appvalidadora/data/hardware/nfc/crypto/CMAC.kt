package mx.com.rutamovil.appvalidadora.data.hardware.nfc.crypto

import mx.com.rutamovil.appvalidadora.common.util.Utils

/**
 * Implementación del algoritmo de Código de Autenticación de Mensajes basado en Cifrado (CMAC).
 * Utilizado para garantizar la integridad y autenticidad de los datos transmitidos al chip.
 * Soportado según las especificaciones de NIST SP 800-38B.
 */
object CMAC {
    private const val Rb64: Byte = 0x1B
    private const val Rb128: Byte = 0x87.toByte()

    /** Definición de los tipos de cifrado base soportados para el cálculo de CMAC. */
    enum class Type {
        TKTDES, AES
    }

    /**
     * Calcula el valor CMAC utilizando un vector de inicialización nulo por defecto.
     *
     * @param type Algoritmo base (AES o Triple DES).
     * @param key Llave de sesión utilizada para el cálculo.
     * @param data Datos sobre los cuales se calculará el código de integridad.
     * @return Arreglo de bytes con el código MAC resultante.
     */
    fun get(type: Type, key: ByteArray, data: ByteArray): ByteArray? {
        val zeros = when (type) {
            Type.TKTDES -> ByteArray(8)
            Type.AES -> ByteArray(16)
        }
        return get(type, key, data, zeros)
    }

    /**
     * Calcula el valor CMAC proporcionando un vector de inicialización específico.
     * Realiza la generación de sub-llaves K1 y K2 internamente.
     */
    fun get(type: Type, key: ByteArray, data: ByteArray, aesIv: ByteArray): ByteArray? {
        val blockSize: Int
        val rb: Byte
        var nistL: ByteArray?

        when (type) {
            Type.TKTDES -> {
                blockSize = 8
                rb = Rb64
                val zeros8 = ByteArray(blockSize)
                // TODO: Implementar TripleDES si es requerido en el futuro.
                nistL = null 
            }
            Type.AES -> {
                blockSize = 16
                rb = Rb128
                val zeros16 = ByteArray(blockSize)
                nistL = AES.encrypt(zeros16, key, zeros16)
            }
        }

        if (nistL == null) return null

        val nistK1 = getSubK1(nistL!!, blockSize, rb)
        val nistK2 = getSubK2(nistK1, blockSize, rb)

        return getCMAC(key, nistK1, nistK2, data, aesIv, blockSize, type)
    }

    /** Lógica central de procesamiento por bloques para la generación del CMAC final. */
    private fun getCMAC(k: ByteArray, k1: ByteArray, k2: ByteArray, block: ByteArray, eIv: ByteArray, size: Int, type: Type): ByteArray? {
        var newBlock = block
        if (block.isEmpty()) {
            newBlock = ByteArray(size)
            newBlock[0] = 0x80.toByte()
        }
        if (block.size % size != 0) {
            val index = block.size
            newBlock = ByteArray(block.size - block.size % size + size)
            System.arraycopy(block, 0, newBlock, 0, block.size)
            newBlock[index] = 0x80.toByte()
        }

        if (block.isNotEmpty() && block.size % size == 0) {
            for (i in newBlock.size - size until newBlock.size)
                newBlock[i] = (newBlock[i].toInt() xor k1[i - newBlock.size + size].toInt()).toByte()
        } else {
            for (i in newBlock.size - size until newBlock.size)
                newBlock[i] = (newBlock[i].toInt() xor k2[i - newBlock.size + size].toInt()).toByte()
        }

        val formattedMessage = when (type) {
            Type.TKTDES -> null 
            Type.AES -> AES.encrypt(eIv, k, newBlock)
        }

        if (formattedMessage == null) return null

        val cmac = ByteArray(size)
        System.arraycopy(formattedMessage, formattedMessage.size - size, cmac, 0, size)
        return cmac
    }

    /** Generación de la segunda sub-llave (K2) mediante operaciones de desplazamiento y XOR. */
    private fun getSubK2(k1: ByteArray, size: Int, poly: Byte): ByteArray {
        val rb = ByteArray(size)
        rb[rb.size - 1] = poly
        val k2 = shiftLeft(k1)
        if ((k1[0].toInt() and 0x80) != 0) {
            for (i in 0 until size) {
                k2[i] = (k2[i].toInt() xor rb[i].toInt()).toByte()
            }
        }
        return k2
    }

    /** Generación de la primera sub-llave (K1) a partir del cifrado de un bloque de ceros. */
    private fun getSubK1(l: ByteArray, size: Int, poly: Byte): ByteArray {
        val rb = ByteArray(size)
        rb[rb.size - 1] = poly
        val k1 = shiftLeft(l)
        if ((l[0].toInt() and 0x80) != 0) {
            for (i in 0 until size) {
                k1[i] = (k1[i].toInt() xor rb[i].toInt()).toByte()
            }
        }
        return k1
    }

    private fun shiftLeft(a: ByteArray): ByteArray {
        return toByte(shiftLeft(toBit(a)))
    }

    private fun toByte(s: String): ByteArray {
        val a = ByteArray(s.length / 8)
        for (i in 0 until s.length step 8) {
            a[i / 8] = s.substring(i, i + 8).toInt(2).toByte()
        }
        return a
    }

    private fun shiftLeft(s: String): String = s.substring(1) + "0"

    private fun toBit(a: ByteArray): String {
        val sb = StringBuilder()
        for (b in a) {
            val s = Integer.toBinaryString(0x100 + (b.toInt() and 0xFF))
            sb.append(s.substring(s.length - 8))
        }
        return sb.toString()
    }
}
