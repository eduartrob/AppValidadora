package mx.com.rutamovil.appvalidadora.data.hardware.nfc.file

import android.os.Parcel
import android.os.Parcelable
import java.util.Locale

/**
 * Clase que representa el identificador único de un archivo dentro de una aplicación DESFire.
 * Implementa [Parcelable] para permitir su transferencia entre componentes de Android.
 */
open class DesfireFileId() : Parcelable {
    /** Identificador numérico del archivo (0 a 31). */
    var id: Int = 0

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
    }

    /** Obtiene la representación hexadecimal del identificador. */
    fun getIdString(): String {
        return "0x" + Integer.toHexString(id).uppercase(Locale.ENGLISH)
    }

    /** Lee el estado del objeto desde un Parcel. */
    open fun readFromParcel(source: Parcel) {
        id = source.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
    }

    override fun describeContents(): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DesfireFileId) return false
        return id == other.id
    }

    override fun hashCode(): Int = id

    companion object CREATOR : Parcelable.Creator<DesfireFileId> {
        override fun createFromParcel(parcel: Parcel): DesfireFileId {
            return DesfireFileId(parcel)
        }

        override fun newArray(size: Int): Array<DesfireFileId?> {
            return arrayOfNulls(size)
        }
    }
}
