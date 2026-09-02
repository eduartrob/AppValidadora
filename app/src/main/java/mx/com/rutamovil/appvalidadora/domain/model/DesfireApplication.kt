package mx.com.rutamovil.appvalidadora.domain.model

import android.os.Parcel
import android.os.Parcelable
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.file.DesfireFile
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.DesfireKeyType

/**
 * Representa una aplicación lógica dentro de una tarjeta DESFire.
 * Contiene la colección de archivos, llaves y configuraciones de seguridad asociadas a un AID específico.
 */
class DesfireApplication : DesfireApplicationId {

    companion object {
        /** Cantidad máxima de archivos permitidos por aplicación. */
        const val MAX_FILE_COUNT = 32

        @JvmField
        val CREATOR = object : Parcelable.Creator<DesfireApplication> {
            override fun createFromParcel(parcel: Parcel): DesfireApplication {
                val item = DesfireApplication()
                item.readFromParcel(parcel)
                return item
            }

            override fun newArray(size: Int): Array<DesfireApplication?> = arrayOfNulls(size)
        }
    }

    /** Lista de archivos (Standard, Value, etc.) contenidos en la aplicación. */
    var files: MutableList<DesfireFile> = mutableListOf()
    /** Lista de referencias a las llaves criptográficas configuradas. */
    var keys: MutableList<DesfireApplicationKey> = mutableListOf()
    /** Algoritmo de seguridad principal de la aplicación. */
    var security: DesfireKeyType? = null
    /** Ajustes detallados de acceso y permisos de las llaves. */
    var keySettings: DesfireApplicationKeySettings? = null

    constructor() : super()

    /** Indica si la aplicación tiene archivos registrados. */
    fun hasFiles(): Boolean = files.isNotEmpty()
    /** Indica si la aplicación tiene llaves configuradas. */
    fun hasKeys(): Boolean = keys.isNotEmpty()

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(files.size)
        for (file in files) {
            parcel.writeParcelable(file, flags)
        }

        keySettings?.let {
            parcel.writeByte(0x01)
            parcel.writeParcelable(it, flags)
        } ?: parcel.writeByte(0x00)
    }

    override fun readFromParcel(parcel: Parcel) {
        super.readFromParcel(parcel)
        val count = parcel.readInt()
        files.clear()
        for (i in 0 until count) {
            parcel.readParcelable<DesfireFile>(DesfireFile::class.java.classLoader)?.let {
                files.add(it)
            }
        }

        if (parcel.readByte().toInt() == 0x01) {
            keySettings = parcel.readParcelable(javaClass.classLoader)
        }
    }

    /** Agrega un archivo a la estructura de la aplicación. */
    fun add(file: DesfireFile) {
        files.add(file)
    }

    /** Agrega una referencia de llave a la aplicación. */
    fun add(key: DesfireApplicationKey) {
        keys.add(key)
    }
}
