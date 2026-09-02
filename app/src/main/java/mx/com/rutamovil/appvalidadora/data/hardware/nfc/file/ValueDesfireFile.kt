package mx.com.rutamovil.appvalidadora.data.hardware.nfc.file

import android.os.Parcel
import android.os.Parcelable
import mx.com.rutamovil.appvalidadora.common.util.Utils
import java.io.ByteArrayInputStream

/**
 * Representación especializada de un archivo de tipo "Value" (Valor) en DESFire.
 * Este tipo de archivo se utiliza comúnmente para implementar monederos electrónicos,
 * permitiendo operaciones atómicas de crédito y débito.
 */
class ValueDesfireFile : DesfireFile {

    /** Límite inferior permitido para el valor del archivo. */
    var lowerLimit: Int = 0
    /** Límite superior permitido para el valor del archivo. */
    var upperLimit: Int = 0
    /** Valor límite para operaciones de crédito limitado. */
    var limitedCreditValue: Int = 0
    /** Indica si está habilitada la funcionalidad de crédito limitado. */
    var limitedCredit: Boolean = false
    /** Indica si se permite la lectura del valor sin autenticación previa. */
    var freeGetValue: Boolean = false
    /** El valor actual almacenado en el archivo (saldo). */
    var value: Int? = null

    constructor() : super()

    constructor(id: Int, settings: ByteArrayInputStream) : super() {
        this.id = id
        read(settings)
    }

    /**
     * Lee la configuración específica del archivo de valor desde el flujo de datos.
     */
    override fun read(settings: ByteArrayInputStream) {
        super.read(settings)

        val buf = ByteArray(4)
        settings.read(buf, 0, buf.size)
        reverse(buf)
        lowerLimit = Utils.byteArrayToInt(buf)

        settings.read(buf, 0, buf.size)
        reverse(buf)
        upperLimit = Utils.byteArrayToInt(buf)

        settings.read(buf, 0, buf.size)
        reverse(buf)
        limitedCreditValue = Utils.byteArrayToInt(buf)

        val flags = settings.read()
        limitedCredit = (flags and 0x1) != 0
        freeGetValue = (flags and 0x2) != 0
    }

    constructor(id: Int, fileType: DesfireFileType, commSetting: Byte,
                readAccessKey: Int, writeAccessKey: Int, readWriteAccessKey: Int, changeAccessKey: Int,
                lowerLimit: Int, upperLimit: Int, limitedCreditValue: Int, limitedCredit: Boolean, freeGetValue: Boolean)
            : super(id, fileType, commSetting, readAccessKey, writeAccessKey, readWriteAccessKey, changeAccessKey) {
        this.lowerLimit = lowerLimit
        this.upperLimit = upperLimit
        this.limitedCreditValue = limitedCreditValue
        this.limitedCredit = limitedCredit
        this.freeGetValue = freeGetValue
    }

    override fun readFromParcel(source: Parcel) {
        super.readFromParcel(source)
        lowerLimit = source.readInt()
        upperLimit = source.readInt()
        limitedCreditValue = source.readInt()
        limitedCredit = source.readByte() == 0x1.toByte()
        freeGetValue = source.readByte() == 0x1.toByte()

        if (source.readByte() != 0.toByte()) {
            value = source.readInt()
        } else {
            value = null
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(lowerLimit)
        parcel.writeInt(upperLimit)
        parcel.writeInt(limitedCreditValue)
        parcel.writeByte(if (limitedCredit) 0x1 else 0x0)
        parcel.writeByte(if (freeGetValue) 0x1 else 0x0)

        value?.let {
            parcel.writeByte(1)
            parcel.writeInt(it)
        } ?: parcel.writeByte(0)
    }

    override fun isContent(): Boolean = value != null

    override fun toString(): String {
        return "ValueDesfireFile [lowerLimit=$lowerLimit, upperLimit=$upperLimit, limitedCreditValue=$limitedCreditValue, " +
                "limitedCredit=$limitedCredit, freeGetValue=$freeGetValue, value=$value, fileType=$fileType, " +
                "communicationSettings=$communicationSettings, readAccessKey=$readAccessKey, writeAccessKey=$writeAccessKey, " +
                "readWriteAccessKey=$readWriteAccessKey, changeAccessKey=$changeAccessKey, id=$id]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (!super.equals(other)) return false
        if (other !is ValueDesfireFile) return false

        if (lowerLimit != other.lowerLimit) return false
        if (upperLimit != other.upperLimit) return false
        if (limitedCreditValue != other.limitedCreditValue) return false
        if (limitedCredit != other.limitedCredit) return false
        if (freeGetValue != other.freeGetValue) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + lowerLimit
        result = 31 * result + upperLimit
        result = 31 * result + limitedCreditValue
        result = 31 * result + limitedCredit.hashCode()
        result = 31 * result + freeGetValue.hashCode()
        result = 31 * result + (value ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<ValueDesfireFile> {
        override fun createFromParcel(parcel: Parcel): ValueDesfireFile {
            val file = ValueDesfireFile()
            file.readFromParcel(parcel)
            return file
        }

        override fun newArray(size: Int): Array<ValueDesfireFile?> {
            return arrayOfNulls(size)
        }
    }
}
