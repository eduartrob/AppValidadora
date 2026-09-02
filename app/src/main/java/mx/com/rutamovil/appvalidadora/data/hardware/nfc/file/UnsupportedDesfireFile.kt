package mx.com.rutamovil.appvalidadora.data.hardware.nfc.file

import android.os.Parcel
import android.os.Parcelable
import java.io.ByteArrayInputStream

/**
 * Clase que actúa como contenedor para tipos de archivos DESFire no reconocidos o no implementados.
 * Permite mantener la integridad de la lista de archivos sin procesar su contenido específico.
 */
class UnsupportedDesfireFile : DesfireFile {

    constructor() : super()

    constructor(id: Int, settings: ByteArrayInputStream) : super() {
        this.id = id
        read(settings)
    }

    constructor(id: Int, fileType: DesfireFileType, commSetting: Byte,
                readAccessKey: Int, writeAccessKey: Int, readWriteAccessKey: Int, changeAccessKey: Int)
            : super(id, fileType, commSetting, readAccessKey, writeAccessKey, readWriteAccessKey, changeAccessKey)

    override fun read(settings: ByteArrayInputStream) {
        super.read(settings)
    }

    override fun isContent(): Boolean = false

    companion object CREATOR : Parcelable.Creator<UnsupportedDesfireFile> {
        override fun createFromParcel(parcel: Parcel): UnsupportedDesfireFile {
            val file = UnsupportedDesfireFile()
            file.readFromParcel(parcel)
            return file
        }

        override fun newArray(size: Int): Array<UnsupportedDesfireFile?> {
            return arrayOfNulls(size)
        }
    }
}
