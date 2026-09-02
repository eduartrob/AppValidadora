package mx.com.rutamovil.appvalidadora.data.hardware.nfc.file

import android.os.Parcel
import android.os.Parcelable
import mx.com.rutamovil.appvalidadora.common.util.Utils
import java.io.ByteArrayInputStream
import java.util.Arrays

/**
 * Clase que representa archivos de tipo registro (Linear o Cyclic) en una tarjeta DESFire.
 * Estos archivos organizan la información en bloques estructurados de tamaño uniforme.
 */
class RecordDesfireFile : DesfireFile {

    /** Tamaño en bytes de cada registro individual. */
    var recordSize: Int = 0
    /** Cantidad máxima de registros que puede contener el archivo. */
    var maxRecords: Int = 0
    /** Cantidad actual de registros almacenados. */
    var currentRecords: Int = 0
    /** Contenido binario de los registros leídos. */
    var records: ByteArray? = null

    constructor() : super()

    constructor(id: Int, settings: ByteArrayInputStream) : super() {
        this.id = id
        read(settings)
    }

    /** Lee la configuración de la estructura de registros desde el flujo de entrada. */
    override fun read(settings: ByteArrayInputStream) {
        super.read(settings)

        val buf = ByteArray(3)
        settings.read(buf, 0, buf.size)
        reverse(buf)
        recordSize = Utils.byteArrayToInt(buf)

        settings.read(buf, 0, buf.size)
        reverse(buf)
        maxRecords = Utils.byteArrayToInt(buf)

        settings.read(buf, 0, buf.size)
        reverse(buf)
        currentRecords = Utils.byteArrayToInt(buf)
    }

    constructor(id: Int, fileType: DesfireFileType, commSetting: Byte,
                readAccessKey: Int, writeAccessKey: Int, readWriteAccessKey: Int, changeAccessKey: Int,
                recordSize: Int, maxRecords: Int, curRecords: Int)
            : super(id, fileType, commSetting, readAccessKey, writeAccessKey, readWriteAccessKey, changeAccessKey) {
        this.recordSize = recordSize
        this.maxRecords = maxRecords
        this.currentRecords = curRecords
    }

    override fun readFromParcel(source: Parcel) {
        super.readFromParcel(source)
        recordSize = source.readInt()
        maxRecords = source.readInt()
        currentRecords = source.readInt()

        val size = source.readInt()
        if (size > 0) {
            records = ByteArray(size)
            source.readByteArray(records!!)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(recordSize)
        parcel.writeInt(maxRecords)
        parcel.writeInt(currentRecords)

        records?.let {
            parcel.writeInt(it.size)
            parcel.writeByteArray(it)
        } ?: parcel.writeInt(0)
    }

    override fun isContent(): Boolean = records != null

    override fun toString(): String {
        return "RecordDesfireFile [recordSize=$recordSize, maxRecords=$maxRecords, currentRecords=$currentRecords, " +
                "records=${Arrays.toString(records)}, fileType=$fileType, communicationSettings=$communicationSettings, " +
                "readAccessKey=$readAccessKey, writeAccessKey=$writeAccessKey, " +
                "readWriteAccessKey=$readWriteAccessKey, changeAccessKey=$changeAccessKey, id=$id]"
    }

    companion object CREATOR : Parcelable.Creator<RecordDesfireFile> {
        override fun createFromParcel(parcel: Parcel): RecordDesfireFile {
            val file = RecordDesfireFile()
            file.readFromParcel(parcel)
            return file
        }

        override fun newArray(size: Int): Array<RecordDesfireFile?> {
            return arrayOfNulls(size)
        }
    }
}
