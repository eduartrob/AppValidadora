package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key

import android.os.Parcel
import android.os.Parcelable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Implementación de una llave criptográfica de tipo AES (128 bits) para DESFire.
 * Garantiza que el valor de la llave tenga siempre una longitud de 16 bytes.
 */
class DesfireAESKey : DesfireKey {

    companion object {
        /** Instancia de llave AES nula por defecto con versión 0x01. */
        val defaultVersionNull = DesfireAESKey("AES null", 0x01, ByteArray(16))
        /** Instancia de llave AES por defecto con versión 0x42. */
        val defaultVersion42 = DesfireAESKey("Default AES", 0x42, ByteArray(16))

        @JvmField
        val CREATOR = object : Parcelable.Creator<DesfireAESKey> {
            override fun createFromParcel(parcel: Parcel): DesfireAESKey = DesfireAESKey(parcel)
            override fun newArray(size: Int): Array<DesfireAESKey?> = arrayOfNulls(size)
        }
    }

    constructor() : super() {
        this.type = DesfireKeyType.AES
    }

    constructor(name: String, version: Int, value: ByteArray) : this() {
        this.name = name
        this.version = version
        if (value.size != 16) {
            throw IllegalArgumentException("La longitud de la llave AES debe ser de 16 bytes")
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
            if (v != null && v.size != 16) {
                throw IllegalArgumentException("La longitud de la llave AES debe ser de 16 bytes")
            }
            field = v
        }

    @Throws(IOException::class)
    override fun read(input: DataInputStream) {
        super.read(input)
        val newValue = ByteArray(16)
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
