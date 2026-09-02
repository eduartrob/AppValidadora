package mx.com.rutamovil.appvalidadora.data.hardware.nfc

import android.util.Log
import mx.com.rutamovil.appvalidadora.common.util.BitOp
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.crypto.*
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.command.CommandBuilder
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.file.*
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.random.DefaultRandomSource
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.random.RandomSource
import java.io.IOException
import java.util.Arrays

/**
 * Motor de comandos y lógica de protocolo para tarjetas MIFARE DESFire EV1.
 * Implementa las operaciones de bajo nivel requeridas para la gestión de aplicaciones,
 * archivos, seguridad criptográfica y transacciones monetarias dentro del chip.
 */
class DESFireEV1 {

    companion object {
        /** Constante para aplicaciones con criptografía DES. */
        const val APPLICATION_CRYPTO_DES: Byte = 0x00
        /** Constante para aplicaciones con criptografía 3K3DES. */
        const val APPLICATION_CRYPTO_3K3DES: Byte = 0x40
        /** Constante para aplicaciones con criptografía AES. */
        const val APPLICATION_CRYPTO_AES: Byte = 0x80.toByte()
        /** Límite máximo de archivos permitidos por aplicación en DESFire. */
        const val MAX_FILE_COUNT = 32
        private val TAG = DESFireEV1::class.java.name
        private const val FAKE_NO: Byte = -1

        /**
         * Genera una llave de sesión basada en los números aleatorios compartidos durante la autenticación.
         *
         * @param randA Número aleatorio generado por el lector.
         * @param randB Número aleatorio generado por la tarjeta.
         * @param type Tipo de algoritmo criptográfico utilizado.
         * @return Arreglo de bytes que representa la llave de sesión generada.
         */
        private fun generateSessionKey(randA: ByteArray, randB: ByteArray, type: KeyType): ByteArray {
            return when (type) {
                KeyType.DES -> {
                    val skey = ByteArray(8)
                    System.arraycopy(randA, 0, skey, 0, 4)
                    System.arraycopy(randB, 0, skey, 4, 4)
                    skey
                }
                KeyType.TDES -> {
                    val skey = ByteArray(16)
                    System.arraycopy(randA, 0, skey, 0, 4)
                    System.arraycopy(randB, 0, skey, 4, 4)
                    System.arraycopy(randA, 4, skey, 8, 4)
                    System.arraycopy(randB, 4, skey, 12, 4)
                    skey
                }
                KeyType.TKTDES -> {
                    val skey = ByteArray(24)
                    System.arraycopy(randA, 0, skey, 0, 4)
                    System.arraycopy(randB, 0, skey, 4, 4)
                    System.arraycopy(randA, 6, skey, 8, 4)
                    System.arraycopy(randB, 6, skey, 12, 4)
                    System.arraycopy(randA, 12, skey, 16, 4)
                    System.arraycopy(randB, 12, skey, 20, 4)
                    skey
                }
                KeyType.AES -> {
                    val skey = ByteArray(16)
                    System.arraycopy(randA, 0, skey, 0, 4)
                    System.arraycopy(randB, 0, skey, 4, 4)
                    System.arraycopy(randA, 12, skey, 8, 4)
                    System.arraycopy(randB, 12, skey, 12, 4)
                    skey
                }
            }
        }

        /**
         * Cifra datos para su envío a la tarjeta según el tipo de llave y vector de inicialización.
         */
        private fun send(key: ByteArray, data: ByteArray, type: KeyType, iv: ByteArray?): ByteArray? {
            return when (type) {
                KeyType.DES, KeyType.TDES -> cryptLegacy(key, data, DESMode.SEND_MODE)
                KeyType.TKTDES -> TripleDES.encrypt(iv ?: ByteArray(8), key, data)
                KeyType.AES -> AES.encrypt(iv ?: ByteArray(16), key, data)
            }
        }

        /**
         * Descifra datos recibidos de la tarjeta según el tipo de llave y vector de inicialización.
         */
        private fun recv(key: ByteArray, data: ByteArray, type: KeyType, iv: ByteArray?): ByteArray? {
            return when (type) {
                KeyType.DES, KeyType.TDES -> cryptLegacy(key, data, DESMode.RECEIVE_MODE)
                KeyType.TKTDES -> TripleDES.decrypt(iv ?: ByteArray(8), key, data)
                KeyType.AES -> AES.decrypt(iv ?: ByteArray(16), key, data)
            }
        }

        /**
         * Implementación de cifrado para algoritmos heredados (DES y TDES) con encadenamiento CBC manual.
         */
        private fun cryptLegacy(key: ByteArray, data: ByteArray, mode: DESMode): ByteArray? {
            val modifiedKey = ByteArray(24)
            System.arraycopy(key, 0, modifiedKey, 16, 8)
            System.arraycopy(key, 0, modifiedKey, 8, 8)
            System.arraycopy(key, 0, modifiedKey, 0, key.size)

            val ciphertext = ByteArray(data.size)
            var cipheredBlock = ByteArray(8)

            when (mode) {
                DESMode.SEND_MODE -> {
                    for (i in data.indices step 8) {
                        for (j in 0 until 8) {
                            data[i + j] = (data[i + j].toInt() xor cipheredBlock[j].toInt()).toByte()
                        }
                        cipheredBlock = TripleDES.encrypt(ByteArray(8), modifiedKey, data, i, 8) ?: return null
                        System.arraycopy(cipheredBlock, 0, ciphertext, i, 8)
                    }
                }
                DESMode.RECEIVE_MODE -> {
                    cipheredBlock = TripleDES.decrypt(ByteArray(8), modifiedKey, data, 0, 8) ?: return null
                    System.arraycopy(cipheredBlock, 0, ciphertext, 0, 8)
                    for (i in 8 until data.size step 8) {
                        cipheredBlock = TripleDES.decrypt(ByteArray(8), modifiedKey, data, i, 8) ?: return null
                        for (j in 0 until 8) {
                            cipheredBlock[j] = (cipheredBlock[j].toInt() xor data[i + j - 8].toInt()).toByte()
                        }
                        System.arraycopy(cipheredBlock, 0, ciphertext, i, 8)
                    }
                }
            }
            return ciphertext
        }

        /** Realiza una rotación de un byte hacia la izquierda en un arreglo. */
        private fun rotateLeft(a: ByteArray): ByteArray {
            val ret = ByteArray(a.size)
            System.arraycopy(a, 1, ret, 0, a.size - 1)
            ret[a.size - 1] = a[0]
            return ret
        }

        /** Extrae el byte de estado SW1 de una respuesta APDU. */
        fun getSW1(responseAPDU: ByteArray): Int = responseAPDU[responseAPDU.size - 2].toInt() and 0xff
        
        /** Extrae el byte de estado SW2 de una respuesta APDU. Representa el código de resultado. */
        fun getSW2(responseAPDU: ByteArray): Int = responseAPDU[responseAPDU.size - 1].toInt() and 0xff
        
        /** Extrae el cuerpo de datos de una respuesta APDU, omitiendo los bytes de estado. */
        private fun getData(responseAPDU: ByteArray): ByteArray {
            val data = ByteArray(responseAPDU.size - 2)
            System.arraycopy(responseAPDU, 0, data, 0, data.size)
            return data
        }

        /** Valida que la longitud de la llave sea consistente con su tipo algorítmico. */
        fun validateKey(key: ByteArray, type: KeyType): Boolean {
            if (type == KeyType.DES && key.size != 8 ||
                type == KeyType.TDES && (key.size != 16 || !isKey3DES(key)) ||
                type == KeyType.TKTDES && key.size != 24 ||
                type == KeyType.AES && key.size != 16) {
                Log.e(TAG, "Key validation failed: length is ${key.size} and type is $type")
                return false
            }
            return true
        }

        /** Determina si una llave de 16 bytes califica como Triple DES. */
        fun isKey3DES(key: ByteArray): Boolean {
            if (key.size != 16) return false
            val tmpKey = key.copyOf()
            setKeyVersion(tmpKey, 0, tmpKey.size, 0x00)
            for (i in 0 until 8) {
                if (tmpKey[i] != tmpKey[i + 8]) return true
            }
            return false
        }

        /** Establece los bits de versión dentro de una llave DES o TDES. */
        private fun setKeyVersion(a: ByteArray, offset: Int, length: Int, version: Byte) {
            if (length == 8 || length == 16 || length == 24) {
                for (i in offset + length - 1 downTo offset) {
                    val j = (offset + length - 1 - i) % 8
                    a[i] = (a[i].toInt() and 0xFE).toByte()
                    a[i] = (a[i].toInt() or ((version.toInt() ushr j) and 0x01)).toByte()
                }
            }
        }
    }

    private var ktype: KeyType? = null
    private var kno: Byte = FAKE_NO
    private var aid: ByteArray = ByteArray(3)
    private var iv: ByteArray? = null
    private var skey: ByteArray? = null
    
    /** Almacena el último código de estado (SW2) devuelto por la tarjeta. */
    var code: Int = 0
        private set
        
    private var adapter: DESFireAdapter? = null
    private var randomSource: RandomSource = DefaultRandomSource()
    private var print: Boolean = false
    private var debug: Boolean = false
    private val fileSettings = arrayOfNulls<DesfireFile>(MAX_FILE_COUNT)

    constructor() {
        reset()
        aid = ByteArray(3)
    }

    constructor(adapter: DESFireAdapter) : this() {
        this.adapter = adapter
    }

    /** Restablece el estado criptográfico de la sesión actual. */
    fun reset() {
        ktype = null
        kno = FAKE_NO
        iv = null
        skey = null
        fileSettings.fill(null)
    }

    /**
     * Inicia un proceso de autenticación de 3 pasos con la tarjeta.
     * Soporta algoritmos DES, TDES, 3K3DES y AES.
     *
     * @param key Llave criptográfica.
     * @param keyNo Índice de la llave dentro de la aplicación seleccionada.
     * @param type Tipo de algoritmo de la llave.
     * @return Verdadero si la autenticación mutua fue exitosa.
     */
    @Throws(IOException::class)
    fun authenticate(key: ByteArray, keyNo: Byte, type: KeyType): Boolean {
        if (!validateKey(key, type)) throw IllegalArgumentException()
        if (type != KeyType.AES) setKeyVersion(key, 0, key.size, 0x00.toByte())

        val iv0 = if (type == KeyType.AES) ByteArray(16) else ByteArray(8)
        var apdu = ByteArray(7)
        apdu[0] = 0x90.toByte()
        apdu[1] = when (type) {
            KeyType.DES, KeyType.TDES -> 0x0A.toByte()
            KeyType.TKTDES -> Command.AUTHENTICATE_3K3DES.code.toByte()
            KeyType.AES -> Command.AUTHENTICATE_AES.code.toByte()
        }
        apdu[4] = 0x01
        apdu[5] = keyNo

        var responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        if (this.code != 0xAF) return false

        val responseData = getData(responseAPDU)
        val randB = recv(key, responseData, type, iv0) ?: return false
        val randBr = rotateLeft(randB)
        val randA = ByteArray(randB.size)
        randomSource.fillRandom(randA)

        val plaintext = ByteArray(randA.size + randBr.size)
        System.arraycopy(randA, 0, plaintext, 0, randA.size)
        System.arraycopy(randBr, 0, plaintext, randA.size, randBr.size)

        val iv1 = responseData.copyOfRange(responseData.size - iv0.size, responseData.size)
        val ciphertext = send(key, plaintext, type, iv1) ?: return false

        apdu = ByteArray(5 + ciphertext.size + 1)
        apdu[0] = 0x90.toByte()
        apdu[1] = 0xAF.toByte()
        apdu[4] = ciphertext.size.toByte()
        System.arraycopy(ciphertext, 0, apdu, 5, ciphertext.size)

        responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        if (this.code != 0x00) return false

        val iv2 = ciphertext.copyOfRange(ciphertext.size - iv0.size, ciphertext.size)
        val randAr = recv(key, getData(responseAPDU), type, iv2) ?: return false
        val randAr2 = rotateLeft(randA)
        if (!randAr.contentEquals(randAr2)) return false

        val skey = generateSessionKey(randA, randB, type)
        this.ktype = type
        this.kno = keyNo
        this.iv = iv0
        this.skey = skey

        return true
    }

    /**
     * Selecciona una aplicación dentro de la tarjeta mediante su AID.
     *
     * @param aid Arreglo de 3 bytes con el Application Identifier.
     * @return Verdadero si la aplicación fue seleccionada con éxito.
     */
    @Throws(IOException::class)
    fun selectApplication(aid: ByteArray): Boolean {
        val apdu = ByteArray(9)
        apdu[0] = 0x90.toByte()
        apdu[1] = Command.SELECT_APPLICATION.code.toByte()
        apdu[4] = 0x03
        System.arraycopy(aid, 0, apdu, 5, 3)

        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        reset()
        if (this.code != 0x00) return false
        this.aid = aid
        return true
    }

    /** Realiza un borrado completo de la tarjeta (Nivel PICC). Requiere privilegios adecuados. */
    @Throws(IOException::class)
    fun formatPICC(): Boolean {
        val apdu = byteArrayOf(0x90.toByte(), Command.FORMAT_PICC.code.toByte(), 0x00, 0x00, 0x00)
        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        return this.code == 0x00
    }

    /**
     * Lee el valor numérico almacenado en un Value File.
     *
     * @param fileNo Índice del archivo.
     * @return Valor entero leído o nulo si ocurrió un error.
     */
    @Throws(Exception::class)
    fun getValue(fileNo: Byte): Int? {
        val apdu = byteArrayOf(0x90.toByte(), Command.GET_VALUE.code.toByte(), 0x00, 0x00, 0x01, fileNo, 0x00)
        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        if (this.code != 0x00) return null
        val data = getData(responseAPDU)
        return if (data.size >= 4) BitOp.lsbToInt(data, 0) else null
    }

    /**
     * Lee una secuencia de bytes de un archivo de datos.
     *
     * @param fileNo Índice del archivo.
     * @param offset Posición inicial de lectura.
     * @param length Cantidad de bytes a leer.
     * @return Arreglo de bytes con la información leída o nulo si falla.
     */
    @Throws(Exception::class)
    fun readData(fileNo: Byte, offset: Int, length: Int): ByteArray? {
        val payload = CommandBuilder(7).bytes1(fileNo).bytes3Lsb(offset).bytes3Lsb(length).bytes()
        val apdu = ByteArray(13)
        apdu[0] = 0x90.toByte()
        apdu[1] = Command.READ_DATA.code.toByte()
        apdu[4] = 0x07
        System.arraycopy(payload, 0, apdu, 5, 7)
        val responseAPDU = adapter!!.transmitChain(apdu)
        this.code = getSW2(responseAPDU)
        return if (this.code == 0x00) getData(responseAPDU) else null
    }

    /** Escribe datos de forma genérica en el contexto actual. */
    @Throws(Exception::class)
    fun writeData(data: ByteArray): Boolean {
        val apdu = ByteArray(5 + data.size + 1)
        apdu[0] = 0x90.toByte()
        apdu[1] = Command.WRITE_DATA.code.toByte()
        apdu[4] = data.size.toByte()
        System.arraycopy(data, 0, apdu, 5, data.size)
        val responseAPDU = adapter!!.transmitChain(apdu)
        this.code = getSW2(responseAPDU)
        return this.code == 0x00
    }

    /** Escribe datos en un archivo específico con desplazamiento. */
    @Throws(Exception::class)
    fun writeData(fileNo: Byte, offset: Int, data: ByteArray): Boolean {
        val payload = ByteArray(7 + data.size)
        payload[0] = fileNo
        // offset 3 bytes LSB
        payload[1] = (offset and 0xFF).toByte()
        payload[2] = (offset shr 8 and 0xFF).toByte()
        payload[3] = (offset shr 16 and 0xFF).toByte()
        // length 3 bytes LSB
        payload[4] = (data.size and 0xFF).toByte()
        payload[5] = (data.size shr 8 and 0xFF).toByte()
        payload[6] = (data.size shr 16 and 0xFF).toByte()
        System.arraycopy(data, 0, payload, 7, data.size)
        return writeData(payload)
    }

    /** Crea una nueva aplicación con criptografía DES por defecto. */
    @Throws(IOException::class)
    fun createApplication(aid: ByteArray, amks: Byte, numberOfKeys: Byte): Boolean {
        return createApplication(aid, amks, KeyType.DES, numberOfKeys)
    }

    /**
     * Crea una nueva aplicación especificando el tipo de criptografía y cantidad de llaves.
     */
    @Throws(IOException::class)
    fun createApplication(aid: ByteArray, amks: Byte, keyType: KeyType, numberOfKeys: Byte): Boolean {
        val apdu = ByteArray(11)
        apdu[0] = 0x90.toByte()
        apdu[1] = Command.CREATE_APPLICATION.code.toByte()
        apdu[4] = 0x05
        System.arraycopy(aid, 0, apdu, 5, 3)
        apdu[8] = amks
        apdu[9] = when (keyType) {
            KeyType.AES -> (numberOfKeys.toInt() or APPLICATION_CRYPTO_AES.toInt()).toByte()
            KeyType.TKTDES -> (numberOfKeys.toInt() or APPLICATION_CRYPTO_3K3DES.toInt()).toByte()
            else -> numberOfKeys
        }
        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        return this.code == 0x00
    }

    /** Crea un archivo de tipo valor (monedero) con límites definidos. */
    @Throws(IOException::class)
    fun createValueFile(fileNo: Byte, commSett: Byte, accessRights: Short, lowerLimit: Int, upperLimit: Int, value: Int, limitedCredit: Byte): Boolean {
        val payload = ByteArray(17)
        payload[0] = fileNo
        payload[1] = commSett
        payload[2] = (accessRights.toInt() shr 8 and 0xFF).toByte()
        payload[3] = (accessRights.toInt() and 0xFF).toByte()
        // limits and value are 4 bytes LSB
        BitOp.intToLsb(lowerLimit, payload, 4)
        BitOp.intToLsb(upperLimit, payload, 8)
        BitOp.intToLsb(value, payload, 12)
        payload[16] = limitedCredit
        return createValueFile(payload)
    }

    /** Crea un archivo de valor a partir de un bloque de datos preconfigurado. */
    @Throws(IOException::class)
    fun createValueFile(payload: ByteArray): Boolean {
        val apdu = ByteArray(23)
        apdu[0] = 0x90.toByte()
        apdu[1] = 0xCC.toByte()
        apdu[4] = 0x11
        System.arraycopy(payload, 0, apdu, 5, 17)
        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        return this.code == 0x00
    }

    /** Crea un archivo de datos estándar dentro de la aplicación seleccionada. */
    @Throws(IOException::class)
    fun createStdDataFile(fileNo: Byte, commSett: Byte, accessRights: Short, fileSize: Int): Boolean {
        val payload = ByteArray(7)
        payload[0] = fileNo
        payload[1] = commSett
        payload[2] = (accessRights.toInt() shr 8 and 0xFF).toByte()
        payload[3] = (accessRights.toInt() and 0xFF).toByte()
        // fileSize is 3 bytes LSB
        payload[4] = (fileSize and 0xFF).toByte()
        payload[5] = (fileSize shr 8 and 0xFF).toByte()
        payload[6] = (fileSize shr 16 and 0xFF).toByte()
        return createStdDataFile(payload)
    }

    /** Crea un archivo de datos estándar a partir de un bloque de configuración. */
    @Throws(IOException::class)
    fun createStdDataFile(payload: ByteArray): Boolean {
        val apdu = ByteArray(13)
        apdu[0] = 0x90.toByte()
        apdu[1] = Command.CREATE_STD_DATA_FILE.code.toByte()
        apdu[4] = 0x07
        System.arraycopy(payload, 0, apdu, 5, 7)
        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        return this.code == 0x00
    }

    /**
     * Descuenta una cantidad del archivo de valor. 
     * Requiere que el archivo esté configurado como de tipo Valor.
     *
     * @param fileNo Índice del archivo de saldo.
     * @param value Cantidad en centavos a descontar.
     * @return Verdadero si el comando fue aceptado por la tarjeta.
     */
    @Throws(IOException::class)
    fun debit(fileNo: Byte, value: Int): Boolean {
        val commSett = getFileCommSett(fileNo, false, false, false, true) ?: return false
        val data = ByteArray(5)
        data[0] = fileNo
        BitOp.intToLsb(value, data, 1)

        val apdu = ByteArray(5 + data.size + 1)
        apdu[0] = 0x90.toByte()
        apdu[1] = Command.DEBIT.code.toByte()
        apdu[4] = data.size.toByte()
        System.arraycopy(data, 0, apdu, 5, data.size)
        apdu[apdu.size - 1] = 0x00

        val processedApdu = preprocess(apdu, 5, commSett) ?: return false
        val responseAPDU = transmit(processedApdu)
        this.code = getSW2(responseAPDU)

        if (this.code == 0x00) {
            postprocess(responseAPDU, 0, commSett)
            return true
        }
        return false
    }

    /** Confirma las operaciones pendientes en la tarjeta, haciendo efectivos los cambios de saldo. */
    @Throws(IOException::class)
    fun commitTransaction(): Boolean {
        val apdu = byteArrayOf(0x90.toByte(), Command.COMMIT_TRANSACTION.code.toByte(), 0x00, 0x00, 0x00)
        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        return this.code == 0x00
    }

    /** Recupera la configuración de seguridad y acceso de un archivo específico. */
    @Throws(IOException::class)
    fun getFileSettings(fileNo: Int): DesfireFile? {
        val apdu = byteArrayOf(0x90.toByte(), Command.GET_FILE_SETTINGS.code.toByte(), 0x00, 0x00, 0x01, fileNo.toByte(), 0x00)
        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)
        if (this.code != 0x00) return null
        val data = getData(responseAPDU)
        return try {
            val file = DesfireFile.newInstance(fileNo, data)
            fileSettings[fileNo] = file
            file
        } catch (e: Exception) {
            Log.e(TAG, "getFileSettings: Error al analizar la configuración del archivo $fileNo")
            null
        }
    }

    /** Actualiza la caché local de configuraciones de archivo. */
    fun updateFileSett(fileNo: Int, forceUpdate: Boolean): DesfireFile? {
        if (!forceUpdate && fileSettings[fileNo] != null) {
            return fileSettings[fileNo]
        }
        return getFileSettings(fileNo)
    }

    /** Determina los ajustes de comunicación requeridos para una operación de archivo. */
    fun getFileCommSett(fileNo: Byte, rw: Boolean, car: Boolean, r: Boolean, w: Boolean): DesfireFileCommunicationSettings? {
        return getFileCommSett(fileNo, rw, car, r, w, if (kno == FAKE_NO) -1 else kno.toInt() and 0x0F)
    }

    /** Evalúa los permisos de acceso y devuelve la configuración de comunicación. */
    fun getFileCommSett(fileNo: Byte, rw: Boolean, car: Boolean, r: Boolean, w: Boolean, authKeyNo: Int): DesfireFileCommunicationSettings? {
        val file = updateFileSett(fileNo.toInt(), false) ?: return null

        if (authKeyNo == -1) return file.communicationSettings

        if (rw && file.isReadWriteAccess(authKeyNo)) return file.communicationSettings
        if (car && file.isChangeAccess(authKeyNo)) return file.communicationSettings
        if (r && file.isReadAccess(authKeyNo)) return file.communicationSettings
        if (w && file.isWriteAccess(authKeyNo)) return file.communicationSettings

        return if (file.freeReadAccess() || file.freeWriteAccess() || file.freeChangeAccess()) {
            file.communicationSettings
        } else {
            null
        }
    }

    /** Procesa un APDU antes de enviarlo, aplicando cifrado o firma MAC si es requerido. */
    private fun preprocess(apdu: ByteArray, offset: Int, commSett: DesfireFileCommunicationSettings): ByteArray? {
        if (skey == null || commSett == DesfireFileCommunicationSettings.PLAIN) return apdu

        val command = apdu[1]
        val data = if (apdu.size > offset + 1) apdu.copyOfRange(offset, apdu.size - 1) else ByteArray(0)

        when (commSett) {
            DesfireFileCommunicationSettings.PLAIN_MAC -> {
                if (ktype == KeyType.AES || ktype == KeyType.TKTDES) {
                    val mac = calculateApduCMAC(command, data) ?: return null
                    val result = ByteArray(apdu.size + 8)
                    System.arraycopy(apdu, 0, result, 0, apdu.size - 1)
                    System.arraycopy(mac, 0, result, apdu.size - 1, 8)
                    result[4] = (result[4] + 8).toByte()
                    return result
                } else {
                    val mac = calculateApduMACC(command, data) ?: return null
                    val result = ByteArray(apdu.size + 4)
                    System.arraycopy(apdu, 0, result, 0, apdu.size - 1)
                    System.arraycopy(mac, 0, result, apdu.size - 1, 4)
                    result[4] = (result[4] + 4).toByte()
                    return result
                }
            }
            DesfireFileCommunicationSettings.ENCIPHERED -> {
                val ciphertext = encryptApdu(command, data) ?: return null
                val result = ByteArray(5 + ciphertext.size + 1)
                System.arraycopy(apdu, 0, result, 0, 5)
                System.arraycopy(ciphertext, 0, result, 5, ciphertext.size)
                result[4] = ciphertext.size.toByte()
                result[result.size - 1] = 0x00
                return result
            }
            else -> {}
        }
        return apdu
    }

    /** Procesa una respuesta APDU, descifrando o validando integridad según corresponda. */
    private fun postprocess(apdu: ByteArray, length: Int, commSett: DesfireFileCommunicationSettings): ByteArray? {
        if (skey == null || commSett == DesfireFileCommunicationSettings.PLAIN) return apdu

        val data = getData(apdu)
        if (data.isEmpty()) return apdu

        when (commSett) {
            DesfireFileCommunicationSettings.PLAIN_MAC -> {
                if (ktype == KeyType.AES || ktype == KeyType.TKTDES) {
                    if (data.size < 8) return data
                    val mac = data.copyOfRange(data.size - 8, data.size)
                    val actualData = data.copyOfRange(0, data.size - 8)
                    iv = mac
                    return actualData
                } else {
                    if (data.size < 4) return data
                    val actualData = data.copyOfRange(0, data.size - 4)
                    calculateApduMACR(actualData)
                    return actualData
                }
            }
            DesfireFileCommunicationSettings.ENCIPHERED -> {
                val ciphertext = data
                val plaintext = recv(skey!!, ciphertext, ktype!!, iv) ?: return null

                if (iv != null) {
                    iv = ciphertext.copyOfRange(ciphertext.size - iv!!.size, ciphertext.size)
                }

                val crcSize = if (ktype == KeyType.AES || ktype == KeyType.TKTDES) 4 else 2
                if (plaintext.size >= crcSize + length) {
                    // TODO: Validar CRC
                    return plaintext.copyOfRange(0, length)
                }
                return plaintext
            }
            else -> {}
        }
        return data
    }

    /** Aplica relleno (padding) de bytes a un bloque para que coincida con el tamaño de bloque del cifrador. */
    private fun pad(data: ByteArray, blockSize: Int): ByteArray {
        val paddedSize = if (data.size % blockSize == 0) data.size else (data.size / blockSize + 1) * blockSize
        val padded = ByteArray(paddedSize)
        System.arraycopy(data, 0, padded, 0, data.size)
        return padded
    }

    /** Calcula el CMAC para un APDU de salida (AES/3K3DES). */
    private fun calculateApduCMAC(command: Byte, data: ByteArray): ByteArray? {
        val macData = ByteArray(1 + data.size)
        macData[0] = command
        System.arraycopy(data, 0, macData, 1, data.size)
        val macType = if (ktype == KeyType.AES) CMAC.Type.AES else CMAC.Type.TKTDES
        val mac = CMAC.get(macType, skey!!, macData, iv!!) ?: return null
        iv = mac
        return mac.copyOfRange(0, 8)
    }

    /** Calcula el MAC (Heredado) para un APDU de salida. */
    private fun calculateApduMACC(command: Byte, data: ByteArray): ByteArray? {
        val macData = ByteArray(1 + data.size)
        macData[0] = command
        System.arraycopy(data, 0, macData, 1, data.size)
        val mac = calculateMAC(macData)
        return mac?.copyOfRange(0, 4)
    }

    /** Calcula el MAC para una respuesta de entrada. */
    private fun calculateApduMACR(data: ByteArray): ByteArray? {
        val mac = calculateMAC(data)
        return mac?.copyOfRange(0, 4)
    }

    /** Implementación base del cálculo de MAC para protocolos heredados. */
    private fun calculateMAC(data: ByteArray): ByteArray? {
        if (skey == null) return null
        val blockSize = 8
        val padded = pad(data, blockSize)
        val ciphertext = cryptLegacy(skey!!, padded, DESMode.SEND_MODE) ?: return null
        return ciphertext.copyOfRange(ciphertext.size - blockSize, ciphertext.size)
    }

    /** Calcula el CRC16 para comandos. */
    private fun calculateApduCRC16C(command: Byte, data: ByteArray): ByteArray {
        val crcData = ByteArray(1 + data.size)
        crcData[0] = command
        System.arraycopy(data, 0, crcData, 1, data.size)
        return CRC16.get(crcData)
    }

    /** Calcula el CRC16 para respuestas. */
    private fun calculateApduCRC16R(data: ByteArray): ByteArray {
        return CRC16.get(data)
    }

    /** Calcula el CRC32 para comandos. */
    private fun calculateApduCRC32C(command: Byte, data: ByteArray): ByteArray {
        val crcData = ByteArray(1 + data.size)
        crcData[0] = command
        System.arraycopy(data, 0, crcData, 1, data.size)
        return CRC32.get(crcData)
    }

    /** Calcula el CRC32 para respuestas. */
    private fun calculateApduCRC32R(data: ByteArray): ByteArray {
        return CRC32.get(data)
    }

    /** Cifra un APDU completo incluyendo su suma de comprobación (CRC). */
    private fun encryptApdu(command: Byte, data: ByteArray): ByteArray? {
        if (skey == null || ktype == null) return null
        val crc = if (ktype == KeyType.AES || ktype == KeyType.TKTDES) {
            calculateApduCRC32C(command, data)
        } else {
            calculateApduCRC16C(command, data)
        }

        val plaintext = ByteArray(data.size + crc.size)
        System.arraycopy(data, 0, plaintext, 0, data.size)
        System.arraycopy(crc, 0, plaintext, data.size, crc.size)

        val blockSize = if (ktype == KeyType.AES) 16 else 8
        val paddedPlaintext = pad(plaintext, blockSize)
        val ciphertext = send(skey!!, paddedPlaintext, ktype!!, iv) ?: return null

        if (iv != null) {
            iv = ciphertext.copyOfRange(ciphertext.size - iv!!.size, ciphertext.size)
        }
        return ciphertext
    }

    /**
     * Modifica una llave existente en la aplicación seleccionada.
     *
     * @param keyNo Índice de la llave a cambiar.
     * @param type Nuevo tipo algorítmico de la llave.
     * @param newKey Valor de la nueva llave criptográfica.
     * @param oldKey Valor de la llave anterior (requerido si no es la llave autenticada).
     * @return Verdadero si el cambio de llave fue aceptado por el chip.
     */
    @Throws(IOException::class)
    fun changeKey(keyNo: Byte, type: KeyType, newKey: ByteArray, oldKey: ByteArray?): Boolean {
        return changeKeyInternal(keyNo, 0x00.toByte(), type, newKey, oldKey)
    }

    /**
     * Lógica interna para la preparación del bloque cifrado de cambio de llave.
     * Implementa los protocolos específicos de DESFire para la transmisión segura de secretos.
     */
    private fun changeKeyInternal(keyNo: Byte, keyVersion: Byte, type: KeyType, newKey: ByteArray, oldKey: ByteArray?): Boolean {
        if (!validateKey(newKey, type)) {
            this.code = 0x9E // WRONG_ARGUMENT
            Log.e(TAG, "changeKey: Error validando la longitud o tipo de la nueva llave.")
            return false
        }

        val isMasterApp = Arrays.equals(aid, ByteArray(3))
        var kNo = keyNo

        // Ajuste cuando se cambia la llave maestra de la tarjeta (AID 000000)
        if (isMasterApp) {
            if (type == KeyType.TKTDES) kNo = 0x40.toByte()
            else if (type == KeyType.AES) kNo = 0x80.toByte()
        }

        val changingCurrentKey = (kNo.toInt() and 0x0F) == (kno.toInt() and 0x0F)

        if (!changingCurrentKey && oldKey == null) {
            Log.e(TAG, "changeKey: Se requiere la llave vieja (oldKey) si no se está cambiando la llave actualmente autenticada.")
            this.code = 0x9E
            return false
        }

        val nklen = if (type == KeyType.TKTDES) 24 else 16

        // Definir el tamaño del bloque de texto plano basado en el tipo de cifrado de la sesión actual
        val plainLen = if (ktype == KeyType.DES || ktype == KeyType.TDES) {
            if (type == KeyType.TKTDES) 32 else 24
        } else {
            32
        }
        val plaintext = ByteArray(plainLen)

        val newKeyCopy = newKey.copyOf()
        if (type == KeyType.AES) {
            plaintext[16] = keyVersion
        } else {
            setKeyVersion(newKeyCopy, 0, newKeyCopy.size, keyVersion)
        }

        System.arraycopy(newKeyCopy, 0, plaintext, 0, newKeyCopy.size)

        // Las llaves DES de 8 bytes se duplican internamente para manejarlas como 16
        if (type == KeyType.DES) {
            System.arraycopy(newKeyCopy, 0, plaintext, 8, newKeyCopy.size)
        }

        // Si cambiamos una llave diferente a la autenticada, hacemos XOR con la vieja
        if (!changingCurrentKey && oldKey != null) {
            for (i in newKeyCopy.indices) {
                plaintext[i] = (plaintext[i].toInt() xor oldKey[i % oldKey.size].toInt()).toByte()
            }
        }

        val addAesKeyVersionByte = if (type == KeyType.AES) 1 else 0
        var ciphertext: ByteArray? = null

        when (ktype) {
            KeyType.DES, KeyType.TDES -> {
                val crc = CRC16.get(plaintext, 0, nklen + addAesKeyVersionByte)
                System.arraycopy(crc, 0, plaintext, nklen + addAesKeyVersionByte, 2)

                if (!changingCurrentKey) {
                    val crcOld = CRC16.get(newKeyCopy)
                    System.arraycopy(crcOld, 0, plaintext, nklen + addAesKeyVersionByte + 2, 2)
                }
                ciphertext = send(skey!!, plaintext, ktype!!, null)
            }
            KeyType.TKTDES, KeyType.AES -> {
                val tmpForCRC = ByteArray(1 + 1 + nklen + addAesKeyVersionByte)
                tmpForCRC[0] = Command.CHANGE_KEY.code.toByte()
                tmpForCRC[1] = kNo
                System.arraycopy(plaintext, 0, tmpForCRC, 2, nklen + addAesKeyVersionByte)

                val crc = CRC32.get(tmpForCRC)
                System.arraycopy(crc, 0, plaintext, nklen + addAesKeyVersionByte, crc.size)

                if (!changingCurrentKey && oldKey != null) {
                    val crcOld = CRC32.get(newKeyCopy)
                    System.arraycopy(crcOld, 0, plaintext, nklen + addAesKeyVersionByte + 4, crcOld.size)
                }

                ciphertext = send(skey!!, plaintext, ktype!!, iv)
                if (ciphertext != null && iv != null) {
                    // Actualizamos el vector de inicialización global (IV)
                    iv = ciphertext.copyOfRange(ciphertext.size - iv!!.size, ciphertext.size)
                }
            }
            else -> return false
        }

        if (ciphertext == null) {
            Log.e(TAG, "changeKey: Fallo al encriptar el bloque de la nueva llave.")
            return false
        }

        val apdu = ByteArray(5 + 1 + ciphertext.size + 1)
        apdu[0] = 0x90.toByte()
        apdu[1] = Command.CHANGE_KEY.code.toByte()
        apdu[4] = (1 + ciphertext.size).toByte()
        apdu[5] = kNo
        System.arraycopy(ciphertext, 0, apdu, 6, ciphertext.size)

        val responseAPDU = transmit(apdu)
        this.code = getSW2(responseAPDU)

        if (this.code != 0x00) {
            Log.e(TAG, "changeKey: El chip rechazó la configuración. Código SW2: 0x${String.format("%02X", this.code)}")
            return false
        }

        // Si cambiamos la llave autenticada actual, la sesión se invalida y debemos hacer reset()
        if (changingCurrentKey) {
            reset()
        }

        return true
    }

    /** Envía un comando APDU a través del adaptador NFC. */
    private fun transmit(command: ByteArray): ByteArray = adapter!!.transmit(command)

    /** Configura el adaptador de comunicación NFC para esta instancia de DESFire. */
    fun setAdapter(adapter: DESFireAdapter) {
        this.adapter = adapter
    }

    /** Enumeración que define el catálogo de comandos soportados por el protocolo DESFire. */
    enum class Command(val code: Int) {
        AUTHENTICATE_DES_2K3DES(0x0A),
        AUTHENTICATE_3K3DES(0x1A),
        AUTHENTICATE_AES(0xAA),
        CHANGE_KEY_SETTINGS(0x54),
        CHANGE_KEY(0xC4),
        GET_KEY_VERSION(0x64),
        CREATE_APPLICATION(0xCA),
        DELETE_APPLICATION(0xDA),
        GET_APPLICATIONS_IDS(0x6A),
        FREE_MEMORY(0x6E),
        GET_DF_NAMES(0x6D),
        GET_KEY_SETTINGS(0x45),
        SELECT_APPLICATION(0x5A),
        FORMAT_PICC(0xFC),
        GET_VERSION(0x60),
        GET_CARD_UID(0x51),
        GET_FILE_IDS(0x6F),
        GET_FILE_SETTINGS(0xF5),
        CHANGE_FILE_SETTINGS(0x5F),
        CREATE_STD_DATA_FILE(0xCD),
        CREATE_BACKUP_DATA_FILE(0xCB),
        CREATE_VALUE_FILE(0xCC),
        CREATE_LINEAR_RECORD_FILE(0xC1),
        CREATE_CYCLIC_RECORD_FILE(0xC0),
        DELETE_FILE(0xDF),
        READ_DATA(0xBD),
        WRITE_DATA(0x3D),
        GET_VALUE(0x6C),
        CREDIT(0x0C),
        DEBIT(0xDC),
        LIMITED_CREDIT(0x1C),
        WRITE_RECORDS(0x3B),
        READ_RECORDS(0xBB),
        CLEAR_RECORD_FILE(0xEB),
        COMMIT_TRANSACTION(0xC7),
        ABORT_TRANSACTION(0xA7)
    }

    private enum class DESMode { SEND_MODE, RECEIVE_MODE }
}
