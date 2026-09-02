package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key

import android.os.Parcel
import android.os.Parcelable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Implementación de una llave criptográfica de tipo Triple DES (2-Key 3DES, 128 bits).
 * Utilizada para la autenticación en aplicaciones DESFire que emplean esquemas heredados de 16 bytes.
 */
class Desfire3DESKey : DesfireKey {

    constructor() : super() {
        this.type = DesfireKeyType.TDES
    }

    constructor(name: String, version: Int, value: ByteArray) : this() {
        this.name = name
        this.version = version
        if (value.size != 16) {
            throw IllegalArgumentException("La longitud de la llave Triple DES (2K) debe ser de 16 bytes")
        }
        this.value = value
    }

    private constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    override var value: ByteArray? = null
        set(v) {
            if (v != null && v.size != 16) {
                throw IllegalArgumentException("La longitud de la llave Triple DES (2K) debe ser de 16 bytes")
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

    companion object {
        /** Llave 3DES nula predeterminada. */
        val defaultVersionNull = Desfire3DESKey("3DES null", 0x01, ByteArray(16))
        /** Llave 3DES de prueba con versión 0xC7. */
        val defaultVersionC7 = Desfire3DESKey(
            "Default 3DES", 0xC7,
            byteArrayOf('C'.toByte(), 'a'.toByte(), 'r'.toByte(), 'd'.toByte(), ' '.toByte(), 'M'.toByte(), 'a'.toByte(), 's'.toByte(), 't'.toByte(), 'e'.toByte(), 'r'.toByte(), ' '.toByte(), 'K'.toByte(), 'e'.toByte(), 'y'.toByte(), '!'.toByte())
        )

        @JvmField
        val CREATOR = object : Parcelable.Creator<Desfire3DESKey> {
            override fun createFromParcel(parcel: Parcel): Desfire3DESKey = Desfire3DESKey(parcel)
            override fun newArray(size: Int): Array<Desfire3DESKey?> = arrayOfNulls(size)
        }
    }
}
