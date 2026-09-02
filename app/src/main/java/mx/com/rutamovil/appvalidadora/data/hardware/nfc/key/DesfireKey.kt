package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key

import android.os.Parcel
import android.os.Parcelable
import mx.com.rutamovil.appvalidadora.common.util.Persistent
import java.io.*
import java.util.*

/**
 * Clase base abstracta para la representación de llaves criptográficas compatibles con MIFARE DESFire.
 * Soporta serialización binaria, parcelización de Android y comparación entre llaves.
 */
abstract class DesfireKey : Parcelable, Comparable<DesfireKey>, Persistent {

    companion object {
        /** Versión del esquema de serialización de la llave. */
        private const val VERSION = 1

        /**
         * Crea una nueva instancia de una llave basada en su tipo y versión.
         *
         * @param type Algoritmo criptográfico de la llave.
         * @param version Número de versión de la llave.
         * @return Instancia concreta de [DesfireKey].
         */
        fun newInstance(type: DesfireKeyType, version: Int): DesfireKey {
            val key = when (type) {
                DesfireKeyType.DES -> DesfireDESKey()
                DesfireKeyType.TDES -> Desfire3DESKey()
                DesfireKeyType.TKTDES -> Desfire3K3DESKey()
                DesfireKeyType.AES -> DesfireAESKey()
                else -> throw IllegalArgumentException()
            }
            key.version = version
            return key
        }

        /**
         * Reconstruye un objeto de llave a partir de su representación en bytes.
         *
         * @param bytes Arreglo de bytes serializados.
         * @return Instancia de [DesfireKey] recuperada.
         */
        @Throws(IOException::class)
        fun fromBytes(bytes: ByteArray): DesfireKey {
            val bis = ByteArrayInputStream(bytes)
            val input = DataInputStream(bis)

            val fileVersion = input.readInt()
            if (fileVersion == VERSION) {
                val type = DesfireKeyType.getType(input.readInt())
                
                bis.reset()
                val key = when (type) {
                    DesfireKeyType.DES -> DesfireDESKey()
                    DesfireKeyType.TDES -> Desfire3DESKey()
                    DesfireKeyType.TKTDES -> Desfire3K3DESKey()
                    DesfireKeyType.AES -> DesfireAESKey()
                    else -> throw IllegalArgumentException()
                }
                key.read(input)
                return key
            } else {
                throw IllegalArgumentException("Unknown version $fileVersion")
            }
        }

        /**
         * Serializa un objeto de llave a un arreglo de bytes.
         */
        @Throws(IOException::class)
        fun toBytes(key: DesfireKey): ByteArray {
            val bos = ByteArrayOutputStream()
            key.write(DataOutputStream(bos))
            return bos.toByteArray()
        }
    }

    /** Versión interna de la llave definida por el usuario o sistema. */
    var version: Int = 0
    /** Algoritmo criptográfico asociado. */
    var type: DesfireKeyType? = null
    /** Identificador numérico opcional para persistencia en base de datos. */
    var id: Long? = null
    /** Nombre descriptivo o alias de la llave. */
    var name: String? = null
    /** Valor binario (secreto) de la llave criptográfica. */
    open var value: ByteArray? = null

    constructor()

    constructor(type: DesfireKeyType, version: Int) {
        this.type = type
        this.version = version
    }

    /** Lee el estado completo de la llave desde un flujo de entrada. */
    @Throws(IOException::class)
    override fun read(input: DataInputStream) {
        val fileVersion = input.readInt()
        if (fileVersion == VERSION) {
            type = DesfireKeyType.getType(input.readInt())
            version = input.readInt()

            if (input.readByte().toInt() != 0) {
                id = input.readLong()
            }

            if (input.readByte().toInt() != 0) {
                name = input.readUTF()
            }
        } else {
            throw IllegalArgumentException("Unknown version $fileVersion")
        }
    }

    /** Escribe el estado actual de la llave en un flujo de salida. */
    @Throws(IOException::class)
    override fun write(output: DataOutputStream) {
        output.writeInt(VERSION)
        output.writeInt(type?.id ?: 0)
        output.writeInt(version)

        id?.let {
            output.writeByte(1)
            output.writeLong(it)
        } ?: output.writeByte(0)

        name?.let {
            output.writeByte(1)
            output.writeUTF(it)
        } ?: output.writeByte(0)
    }

    /** Utilidad interna para deserializar las propiedades base desde un Parcel. */
    protected fun readFromParcel(parcel: Parcel) {
        type = DesfireKeyType.getType(parcel.readInt())
        version = parcel.readInt()

        if (parcel.readByte().toInt() != 0) {
            id = parcel.readLong()
        }

        if (parcel.readByte().toInt() != 0) {
            name = parcel.readString()
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type?.id ?: 0)
        parcel.writeInt(version)

        id?.let {
            parcel.writeByte(1)
            parcel.writeLong(it)
        } ?: parcel.writeByte(0)

        name?.let {
            parcel.writeByte(1)
            parcel.writeString(it)
        } ?: parcel.writeByte(0)
    }

    override fun describeContents(): Int = 0

    /** Convierte la instancia actual a su representación binaria. */
    @Throws(IOException::class)
    fun toBytes(): ByteArray = toBytes(this)

    override fun compareTo(other: DesfireKey): Int {
        var compare = (type?.ordinal ?: 0).compareTo(other.type?.ordinal ?: 0)
        if (compare == 0) {
            compare = (name ?: "").compareTo(other.name ?: "")
            if (compare == 0) {
                return version.compareTo(other.version)
            }
        }
        return compare
    }

    /** Obtiene la versión formateada en hexadecimal. */
    fun getVersionAsHexString(): String = Integer.toHexString(version).uppercase(Locale.ENGLISH)


    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (value?.contentHashCode() ?: 0)
        result = 31 * result + version
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DesfireKey) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (version != other.version) return false
        if (!(value contentEquals other.value)) return false

        return true
    }
}
