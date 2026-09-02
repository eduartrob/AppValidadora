package mx.com.rutamovil.appvalidadora.data.hardware.nfc.file

import android.os.Parcel
import android.os.Parcelable
import java.io.ByteArrayInputStream

/**
 * Clase base abstracta que representa la configuración y metadatos de un archivo dentro de una tarjeta DESFire.
 * Proporciona la estructura común para el manejo de tipos de archivo, configuraciones de comunicación 
 * y derechos de acceso criptográfico.
 */
abstract class DesfireFile : DesfireFileId {

    /** Tipo de archivo según la especificación DESFire. */
    var fileType: DesfireFileType = DesfireFileType.UNKNOWN_FILE_TYPE
    /** Nivel de seguridad requerido para la comunicación con este archivo. */
    var communicationSettings: DesfireFileCommunicationSettings = DesfireFileCommunicationSettings.PLAIN

    /** Índice de la llave requerida para lectura. */
    var readAccessKey: Int = -1
    /** Índice de la llave requerida para escritura. */
    var writeAccessKey: Int = -1
    /** Índice de la llave requerida para operaciones de lectura y escritura. */
    var readWriteAccessKey: Int = -1
    /** Índice de la llave requerida para modificar la configuración del archivo. */
    var changeAccessKey: Int = -1

    constructor() : super()

    constructor(id: Int, fileType: DesfireFileType, communicationSettingsByte: Byte,
                readAccessKey: Int, writeAccessKey: Int, readWriteAccessKey: Int, changeAccessKey: Int) : super() {
        this.id = id
        this.fileType = fileType
        this.communicationSettings = DesfireFileCommunicationSettings.parse(communicationSettingsByte.toInt())
        this.readAccessKey = readAccessKey
        this.writeAccessKey = writeAccessKey
        this.readWriteAccessKey = readWriteAccessKey
        this.changeAccessKey = changeAccessKey
    }

    /**
     * Extrae las configuraciones del archivo a partir de un flujo de bytes devuelto por la tarjeta.
     *
     * @param settings Flujo de entrada con los datos de configuración (GetFileSettings).
     */
    protected open fun read(settings: ByteArrayInputStream) {
        this.fileType = DesfireFileType.getType(settings.read())
        this.communicationSettings = DesfireFileCommunicationSettings.parse(settings.read())

        val access1 = settings.read()
        this.readWriteAccessKey = (access1 and 0xF0) shr 4
        this.changeAccessKey = access1 and 0x0F

        val access2 = settings.read()
        this.readAccessKey = (access2 and 0xF0) shr 4
        this.writeAccessKey = access2 and 0x0F
    }

    override fun readFromParcel(source: Parcel) {
        super.readFromParcel(source)
        this.fileType = DesfireFileType.getType(source.readInt())
        this.communicationSettings = DesfireFileCommunicationSettings.parse(source.readByte().toInt())
        this.readAccessKey = source.readInt()
        this.writeAccessKey = source.readInt()
        this.readWriteAccessKey = source.readInt()
        this.changeAccessKey = source.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(fileType.id)
        parcel.writeByte(communicationSettings.value.toByte())
        parcel.writeInt(readAccessKey)
        parcel.writeInt(writeAccessKey)
        parcel.writeInt(readWriteAccessKey)
        parcel.writeInt(changeAccessKey)
    }

    /** Devuelve una representación textual del tipo de archivo. */
    fun getFileTypeName(): String {
        return when (fileType) {
            DesfireFileType.STANDARD_DATA_FILE -> "Standard"
            DesfireFileType.BACKUP_DATA_FILE -> "Backup"
            DesfireFileType.VALUE_FILE -> "Value"
            DesfireFileType.LINEAR_RECORD_FILE -> "Linear Record"
            DesfireFileType.CYCLIC_RECORD_FILE -> "Cyclic Record"
            else -> "Unknown"
        }
    }

    /** Verifica si el archivo permite acceso de lectura libre (sin autenticación). */
    fun freeReadAccess(): Boolean = readAccessKey == 0xE || readWriteAccessKey == 0xE
    /** Verifica si el archivo permite acceso de escritura libre. */
    fun freeWriteAccess(): Boolean = writeAccessKey == 0xE || readWriteAccessKey == 0xE
    /** Verifica si el archivo permite cambios de configuración libres. */
    fun freeChangeAccess(): Boolean = changeAccessKey == 0xE

    /** Determina si una llave específica tiene permisos de lectura según el índice de autenticación. */
    fun isReadAccess(index: Int): Boolean = readAccessKey == index || readWriteAccessKey == index
    /** Determina si una llave específica tiene permisos de cambio de configuración. */
    fun isChangeAccess(index: Int): Boolean = changeAccessKey == index
    /** Determina si una llave específica tiene permisos de escritura. */
    fun isWriteAccess(index: Int): Boolean = writeAccessKey == index || readWriteAccessKey == index
    /** Determina si una llave específica tiene permisos de lectura y escritura. */
    fun isReadWriteAccess(index: Int): Boolean = readWriteAccessKey == index

    /** Indica si el objeto contiene datos de contenido cargados (además de la configuración). */
    abstract fun isContent(): Boolean

    override fun toString(): String {
        return "${javaClass.name} [fileType=$fileType, communicationSettings=$communicationSettings, " +
                "readAccessKey=$readAccessKey, writeAccessKey=$writeAccessKey, " +
                "readWriteAccessKey=$readWriteAccessKey, changeAccessKey=$changeAccessKey]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!super.equals(other)) return false
        if (other !is DesfireFile) return false

        if (fileType != other.fileType) return false
        if (communicationSettings != other.communicationSettings) return false
        if (readAccessKey != other.readAccessKey) return false
        if (writeAccessKey != other.writeAccessKey) return false
        if (readWriteAccessKey != other.readWriteAccessKey) return false
        if (changeAccessKey != other.changeAccessKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + communicationSettings.hashCode()
        result = 31 * result + readAccessKey
        result = 31 * result + writeAccessKey
        result = 31 * result + readWriteAccessKey
        result = 31 * result + changeAccessKey
        return result
    }

    companion object {
        /** Invierte el orden de los elementos en un arreglo de bytes (cambio de endianness). */
        fun reverse(array: ByteArray?) {
            if (array == null) return
            var i = 0
            var j = array.size - 1
            while (j > i) {
                val tmp = array[j]
                array[j] = array[i]
                array[i] = tmp
                j--
                i++
            }
        }

        /**
         * Factoría para crear la instancia concreta de un archivo DESFire basada en su tipo.
         *
         * @param id Identificador numérico del archivo.
         * @param settings Arreglo de bytes con la configuración cruda.
         * @return Instancia especializada de [DesfireFile].
         */
        @Throws(Exception::class)
        fun newInstance(id: Int, settings: ByteArray): DesfireFile {
            val fileType = DesfireFileType.getType(settings[0].toInt())
            val stream = ByteArrayInputStream(settings)

            return when (fileType) {
                DesfireFileType.STANDARD_DATA_FILE, DesfireFileType.BACKUP_DATA_FILE -> StandardDesfireFile(id, stream)
                DesfireFileType.LINEAR_RECORD_FILE, DesfireFileType.CYCLIC_RECORD_FILE -> RecordDesfireFile(id, stream)
                DesfireFileType.VALUE_FILE -> ValueDesfireFile(id, stream)
                else -> UnsupportedDesfireFile(id, stream)
            }
        }
    }
}
