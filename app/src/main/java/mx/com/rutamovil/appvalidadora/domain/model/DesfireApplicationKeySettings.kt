package mx.com.rutamovil.appvalidadora.domain.model

import android.os.Parcel
import android.os.Parcelable
import mx.com.rutamovil.appvalidadora.data.hardware.nfc.key.DesfireKeyType
import java.util.*

/**
 * Representa los ajustes de configuración de llaves de una aplicación DESFire.
 * Incluye derechos de modificación, visibilidad de directorios y límites de capacidad.
 */
class DesfireApplicationKeySettings : Parcelable {

    /** Derechos de acceso requeridos para cambiar las llaves. */
    var changeKeyAccessRights: Int = 0
    /** Indica si los ajustes de la aplicación pueden ser modificados posteriormente. */
    var isConfigurationChangeable: Boolean = false
    /** Indica si se permite la creación y borrado libre de archivos. */
    var isFreeCreateAndDelete: Boolean = false
    /** Indica si el listado de archivos es visible sin autenticación. */
    var isFreeDirectoryAccess: Boolean = false
    /** Indica si la llave maestra de la aplicación puede ser modificada. */
    var canChangeMasterKey: Boolean = false
    /** Número máximo de llaves soportadas por la aplicación. */
    var maxKeys: Int = 0
    /** Indica si se utilizan identificadores de archivo de dos bytes. */
    var twoByteIdentifiers: Boolean = false
    /** Tipo de algoritmo criptográfico para todas las llaves de la aplicación. */
    var type: DesfireKeyType = DesfireKeyType.NONE
    /** Bloque binario crudo de los ajustes devueltos por el chip. */
    var settings: ByteArray = ByteArray(2)

    constructor()

    /**
     * Construye y parsea los ajustes a partir de un arreglo de 2 bytes (GetKeyStateSettings).
     */
    constructor(settings: ByteArray) {
        if (settings.size < 2) return
        this.settings = byteArrayOf(settings[0], settings[1])

        val s0 = settings[0].toInt() and 0xFF
        isConfigurationChangeable = (s0 and 0x08) != 0
        isFreeCreateAndDelete = (s0 and 0x04) != 0
        isFreeDirectoryAccess = (s0 and 0x02) != 0
        canChangeMasterKey = (s0 and 0x01) != 0
        changeKeyAccessRights = (s0 shr 4) and 0xF

        val s1 = settings[1].toInt() and 0xFF
        maxKeys = s1 and 0x0F
        twoByteIdentifiers = (s1 and 0x20) != 0

        type = when ((s1 shr 6) and 0x3) {
            0x0 -> DesfireKeyType.TDES
            0x1 -> DesfireKeyType.TKTDES
            0x2 -> DesfireKeyType.AES
            else -> DesfireKeyType.NONE
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(settings)
    }

    companion object CREATOR : Parcelable.Creator<DesfireApplicationKeySettings> {
        override fun createFromParcel(parcel: Parcel): DesfireApplicationKeySettings {
            val settings = ByteArray(2)
            parcel.readByteArray(settings)
            return DesfireApplicationKeySettings(settings)
        }

        override fun newArray(size: Int): Array<DesfireApplicationKeySettings?> = arrayOfNulls(size)
    }

    override fun toString(): String {
        return "DesfireApplicationKeySettings [changeKeyAccessRights=$changeKeyAccessRights, " +
                "configurationChangable=$isConfigurationChangeable, freeCreateAndDelete=$isFreeCreateAndDelete, " +
                "freeDirectoryAccess=$isFreeDirectoryAccess, canChangeMasterKey=$canChangeMasterKey, " +
                "maxKeys=$maxKeys, twoByteIdentifiers=$twoByteIdentifiers, type=$type, " +
                "settings=${Arrays.toString(settings)}]"
    }
}
