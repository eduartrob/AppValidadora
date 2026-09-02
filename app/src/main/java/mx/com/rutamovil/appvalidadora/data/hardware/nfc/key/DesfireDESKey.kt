package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key

import android.os.Parcel
import android.os.Parcelable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class DesfireDESKey : DesfireKey {

    companion object {
        val defaultVersionNull = DesfireDESKey("DES null", 0x01, ByteArray(8))
        val defaultVersionAA = DesfireDESKey("Default DES", 0xAA, byteArrayOf('A'.toByte(), 'B'.toByte(), 'C'.toByte(), 'D'.toByte(), 'E'.toByte(), 'F'.toByte(), 'G'.toByte(), 'H'.toByte()))

        @JvmField
        val CREATOR = object : Parcelable.Creator<DesfireDESKey> {
            override fun createFromParcel(parcel: Parcel): DesfireDESKey = DesfireDESKey(parcel)
            override fun newArray(size: Int): Array<DesfireDESKey?> = arrayOfNulls(size)
        }
    }

    constructor() : super() {
        this.type = DesfireKeyType.DES
    }

    constructor(name: String, version: Int, value: ByteArray) : this() {
        this.name = name
        this.version = version
        this.value = value
    }

    constructor(value: ByteArray) : this() {
        if (value.size != 8) {
            throw IllegalArgumentException()
        }
        this.value = value
    }

    private constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
        val valueSize = parcel.readInt()
        if (valueSize != -1) {
            value = ByteArray(valueSize)
            parcel.readByteArray(value!!)
        }
    }

    override var value: ByteArray? = null
        set(v) {
            if (v != null && v.size != 8) {
                throw IllegalArgumentException()
            }
            field = v
        }

    @Throws(IOException::class)
    override fun read(input: DataInputStream) {
        super.read(input)
        val newValue = ByteArray(8)
        input.readFully(newValue)
        this.value = newValue
    }

    @Throws(IOException::class)
    override fun write(output: DataOutputStream) {
        super.write(output)
        value?.let { output.write(it) }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        value?.let {
            parcel.writeInt(it.size)
            parcel.writeByteArray(it)
        } ?: parcel.writeInt(-1)
    }
}
