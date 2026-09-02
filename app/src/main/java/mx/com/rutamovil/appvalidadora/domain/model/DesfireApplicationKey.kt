package mx.com.rutamovil.appvalidadora.domain.model

import android.os.Parcel
import android.os.Parcelable
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.DesfireKey

/**
 * Asociación entre un índice de llave y un objeto de llave criptográfica.
 * Define qué llave se encuentra en una posición específica de la aplicación DESFire.
 */
class DesfireApplicationKey : Parcelable {

    /** Posición o número de la llave dentro de la aplicación. */
    var index: Int = 0
    /** Objeto que contiene el valor y tipo de la llave criptográfica. */
    var desfireKey: DesfireKey? = null

    constructor()

    constructor(index: Int, desfireKey: DesfireKey?) {
        this.index = index
        this.desfireKey = desfireKey
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(index)
        parcel.writeParcelable(desfireKey, flags)
    }

    /** Deserializa los datos de la llave desde un Parcel. */
    fun readFromParcel(parcel: Parcel) {
        index = parcel.readInt()
        desfireKey = parcel.readParcelable(DesfireKey::class.java.classLoader)
    }

    override fun toString(): String = "DesfireKeyReference [index=$index, key=$desfireKey]"

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + (desfireKey?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DesfireApplicationKey) return false
        if (index != other.index) return false
        if (desfireKey != other.desfireKey) return false
        return true
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<DesfireApplicationKey> {
            override fun createFromParcel(parcel: Parcel): DesfireApplicationKey {
                val item = DesfireApplicationKey()
                item.readFromParcel(parcel)
                return item
            }

            override fun newArray(size: Int): Array<DesfireApplicationKey?> = arrayOfNulls(size)
        }
    }
}
