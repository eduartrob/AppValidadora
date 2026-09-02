package mx.com.rutamovil.appvalidadora.domain.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Modelo que representa una tarjeta MIFARE DESFire completa.
 * Actúa como la raíz de la estructura de datos que contiene todas las aplicaciones
 * y la información de versión del hardware.
 */
class DesfireTag : Parcelable {

    companion object {
        /** Límite máximo teórico de aplicaciones en el chip. */
        const val MAX_APPLICATION_COUNT = 28

        @JvmField
        val CREATOR = object : Parcelable.Creator<DesfireTag> {
            override fun createFromParcel(parcel: Parcel): DesfireTag {
                val item = DesfireTag()
                val count = parcel.readInt()
                for (i in 0 until count) {
                    parcel.readParcelable<DesfireApplication>(DesfireApplication::class.java.classLoader)?.let {
                        item.add(it)
                    }
                }
                if (parcel.readByte().toInt() == 0x01) {
                    item.versionInfo = parcel.readParcelable(VersionInfo::class.java.classLoader)
                }
                return item
            }

            override fun newArray(size: Int): Array<DesfireTag?> = arrayOfNulls(size)
        }
    }

    /** Listado de aplicaciones lógicas identificadas durante el escaneo. */
    var applications: MutableList<DesfireApplication> = mutableListOf()
    /** Información técnica detallada del fabricante y modelo del chip. */
    var versionInfo: VersionInfo? = null

    /** Agrega una aplicación a la estructura de la tarjeta. */
    fun add(application: DesfireApplication) {
        applications.add(application)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(applications.size)
        for (application in applications) {
            parcel.writeParcelable(application, flags)
        }

        versionInfo?.let {
            parcel.writeByte(0x01)
            parcel.writeParcelable(it, flags)
        } ?: parcel.writeByte(0x00)
    }
}
