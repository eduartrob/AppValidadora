package mx.com.rutamovil.appvalidadora.domain.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Identificador de Aplicación (AID) para tarjetas MIFARE DESFire.
 * Consiste en una secuencia de 3 bytes que identifica unívocamente una aplicación en el chip.
 */
open class DesfireApplicationId : Parcelable {

    /** Valor binario del identificador (3 bytes). */
    var id: ByteArray = byteArrayOf(0x00, 0x00, 0x00)

    constructor()

    constructor(id: ByteArray) {
        this.id = id
    }

    /** Determina si este identificador corresponde a la aplicación maestra del PICC (000000). */
    fun isMaster(): Boolean = id.size >= 3 && id[0] == 0.toByte() && id[1] == 0.toByte() && id[2] == 0.toByte()

    /** 
     * Obtiene la representación entera del AID.
     * Sigue el ordenamiento de red requerido por ciertos protocolos de hardware.
     */
    fun getIdInt(): Int {
        return (id[0].toInt() and 0xFF shl 16) or (id[1].toInt() and 0xFF shl 8) or (id[2].toInt() and 0xFF)
    }

    /** Obtiene el identificador formateado como cadena hexadecimal. */
    fun getIdString(): String = toHexString(id)

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id.size)
        parcel.writeByteArray(id)
    }

    open fun readFromParcel(parcel: Parcel) {
        val size = parcel.readInt()
        id = ByteArray(size)
        parcel.readByteArray(id)
    }

    companion object {
        /** Convierte un arreglo de bytes a una cadena hexadecimal en mayúsculas. */
        fun toHexString(buffer: ByteArray): String {
            val sb = StringBuilder()
            for (b in buffer) {
                sb.append(String.format("%02X", b.toInt() and 0xFF))
            }
            return sb.toString()
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<DesfireApplicationId> {
            override fun createFromParcel(parcel: Parcel): DesfireApplicationId {
                val item = DesfireApplicationId()
                item.readFromParcel(parcel)
                return item
            }

            override fun newArray(size: Int): Array<DesfireApplicationId?> = arrayOfNulls(size)
        }
    }
}
