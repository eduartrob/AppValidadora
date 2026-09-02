package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key

import android.os.Parcel
import android.os.Parcelable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Implementación de una llave criptográfica de tipo Triple DES (3-Key 3DES, 192 bits).
 * Proporciona el nivel más alto de seguridad dentro del estándar DES heredado para DESFire EV1.
 */
class Desfire3K3DESKey : DesfireKey {

    constructor() : super() {
        this.type = DesfireKeyType.TKTDES
    }

    constructor(name: String, version: Int, value: ByteArray) : this() {
        this.name = name
        this.version = version
        if (value.size != 24) {
            throw IllegalArgumentException("La longitud de la llave Triple DES (3K) debe ser de 24 bytes")
        }
        this.value = value
    }

    private constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    override var value: ByteArray? = null
        set(v) {
            if (v != null && v.size != 24) {
                throw IllegalArgumentException("La longitud de la llave Triple DES (3K) debe ser de 24 bytes")
            }
            field = v
        }

    @Throws(IOException::class)
    override fun read(input: DataInputStream) {
        super.read(input)
        val newValue = ByteArray(24)
        input.readFully(newValue)
        this.value = newValue
    }

    @Throws(IOException::class)
    override fun write(output: DataOutputStream) {
        super.write(output)
        value?.let { output.write(it) }
    }

    companion object {
        /** Llave 3K3DES nula. */
        val defaultVersionNull = Desfire3K3DESKey("3K 3DES null", 0x01, ByteArray(24))
        /** Llave 3K3DES de prueba con versión 0x55. */
        val defaultVersion55 = Desfire3K3DESKey(
            "Default 3K 3DES", 0x55,
            byteArrayOf(0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        )

        @JvmField
        val CREATOR = object : Parcelable.Creator<Desfire3K3DESKey> {
            override fun createFromParcel(parcel: Parcel): Desfire3K3DESKey = Desfire3K3DESKey(parcel)
            override fun newArray(size: Int): Array<Desfire3K3DESKey?> = arrayOfNulls(size)
        }
    }
}
