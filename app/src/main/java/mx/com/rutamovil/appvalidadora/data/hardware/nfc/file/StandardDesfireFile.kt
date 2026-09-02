package mx.com.rutamovil.appvalidadora.data.hardware.nfc.file

import android.os.Parcel
import android.os.Parcelable
import mx.com.rutamovil.appvalidadora.common.util.Utils
import java.io.ByteArrayInputStream
import java.util.Arrays

/**
 * Representa un archivo de datos estándar en una tarjeta DESFire.
 * Se utiliza para almacenar información genérica en un bloque de memoria contiguo.
 */
class StandardDesfireFile : DesfireFile {

    /** Tamaño total del archivo en bytes según la configuración de la tarjeta. */
    var fileSize: Int = 0
    /** Contenido binario almacenado en el archivo. */
    var data: ByteArray? = null

    constructor() : super()

    constructor(id: Int, settings: ByteArrayInputStream) : super() {
        this.id = id
        read(settings)
    }

    /** Lee la configuración del tamaño del archivo desde el flujo de entrada. */
    override fun read(settings: ByteArrayInputStream) {
        super.read(settings)

        val buf = ByteArray(3)
        settings.read(buf, 0, buf.size)
        reverse(buf)
        fileSize = Utils.byteArrayToInt(buf)
    }

    constructor(id: Int, fileType: DesfireFileType, commSetting: Byte,
                readAccessKey: Int, writeAccessKey: Int, readWriteAccessKey: Int, changeAccessKey: Int,
                fileSize: Int)
            : super(id, fileType, commSetting, readAccessKey, writeAccessKey, readWriteAccessKey, changeAccessKey) {
        this.fileSize = fileSize
    }

    override fun readFromParcel(source: Parcel) {
        super.readFromParcel(source)
        fileSize = source.readInt()

        val size = source.readInt()
        if (size > 0) {
            data = ByteArray(size)
            source.readByteArray(data!!)
        } else {
            data = null
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(fileSize)
        data?.let {
            parcel.writeInt(it.size)
            parcel.writeByteArray(it)
        } ?: parcel.writeInt(0)
    }

    override fun isContent(): Boolean = data != null && data!!.isNotEmpty()

    override fun toString(): String {
        return "StandardDesfireFile [fileSize=$fileSize, data=${Arrays.toString(data)}]"
    }

    companion object CREATOR : Parcelable.Creator<StandardDesfireFile> {
        override fun createFromParcel(parcel: Parcel): StandardDesfireFile {
            val file = StandardDesfireFile()
            file.readFromParcel(parcel)
            return file
        }

        override fun newArray(size: Int): Array<StandardDesfireFile?> {
            return arrayOfNulls(size)
        }
    }
}
