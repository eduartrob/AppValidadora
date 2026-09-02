package mx.com.rutamovil.appvalidadora.domain.model

import android.os.Parcel
import android.os.Parcelable
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import kotlin.math.pow

/**
 * Clase que almacena la información de fabricación y versión del chip DESFire.
 * Contiene metadatos sobre el hardware, software, fechas de producción y almacenamiento.
 */
class VersionInfo : Parcelable {

    // Metadatos de Hardware
    var hardwareVendorId: Int = 0
    var hardwareType: Int = 0
    var hardwareSubtype: Int = 0
    var hardwareVersionMajor: Int = 0
    var hardwareVersionMinor: Int = 0
    var hardwareStorageSize: Int = 0
    var hardwareProtocol: Int = 0

    // Metadatos de Software
    var softwareVendorId: Int = 0
    var softwareType: Int = 0
    var softwareSubtype: Int = 0
    var softwareVersionMajor: Int = 0
    var softwareVersionMinor: Int = 0
    var softwareStorageSize: Int = 0
    var softwareProtocol: Int = 0

    /** Identificador único del chip (7 bytes). */
    var uid: ByteArray = ByteArray(7)
    /** Número de lote de producción. */
    var batchNumber: ByteArray = ByteArray(5)
    /** Semana de fabricación. */
    var productionWeek: Int = 0
    /** Año de fabricación. */
    var productionYear: Int = 0
    var productionWeekByte: Byte = 0
    var productionYearByte: Byte = 0

    constructor()

    /**
     * Construye la información de versión a partir de la respuesta del comando GetVersion.
     *
     * @param bytes Arreglo de 28 bytes devuelto por la tarjeta.
     */
    @Throws(IOException::class)
    constructor(bytes: ByteArray) {
        read(bytes)
    }

    /**
     * Parsea los bytes crudos y asigna los valores a los campos correspondientes.
     */
    @Throws(IOException::class)
    fun read(bytes: ByteArray) {
        val din = DataInputStream(ByteArrayInputStream(bytes))

        hardwareVendorId = din.read()
        hardwareType = din.read()
        hardwareSubtype = din.read()
        hardwareVersionMajor = din.read()
        hardwareVersionMinor = din.read()
        hardwareStorageSize = din.read()
        hardwareProtocol = din.read()

        softwareVendorId = din.read()
        softwareType = din.read()
        softwareSubtype = din.read()
        softwareVersionMajor = din.read()
        softwareVersionMinor = din.read()
        softwareStorageSize = din.read()
        softwareProtocol = din.read()

        din.readFully(uid)
        din.readFully(batchNumber)

        productionWeek = din.read()
        productionYear = din.read()
        productionWeekByte = (productionWeek and 0xff).toByte()
        productionYearByte = (productionYear and 0xff).toByte()
    }

    /** Obtiene la cadena de versión de hardware (Major.Minor). */
    fun getHardwareVersion(): String = "$hardwareVersionMajor.$hardwareVersionMinor"
    /** Obtiene la cadena de versión de software (Major.Minor). */
    fun getSoftwareVersion(): String = "$softwareVersionMajor.$softwareVersionMinor"

    /** Calcula la capacidad de almacenamiento de hardware en bytes. */
    fun getCalculatedHardwareStorageSize(): Int {
        return 2.0.pow(hardwareStorageSize shr 1).toInt()
    }

    /** Calcula la capacidad de almacenamiento de software en bytes. */
    fun getCalculatedSoftwareStorageSize(): Int {
        return 2.0.pow(softwareStorageSize shr 1).toInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(hardwareVendorId)
        parcel.writeInt(hardwareType)
        parcel.writeInt(hardwareSubtype)
        parcel.writeInt(hardwareVersionMajor)
        parcel.writeInt(hardwareVersionMinor)
        parcel.writeInt(hardwareStorageSize)
        parcel.writeInt(hardwareProtocol)
        parcel.writeInt(softwareVendorId)
        parcel.writeInt(softwareType)
        parcel.writeInt(softwareSubtype)
        parcel.writeInt(softwareVersionMajor)
        parcel.writeInt(softwareVersionMinor)
        parcel.writeInt(softwareStorageSize)
        parcel.writeInt(softwareProtocol)
        parcel.writeByteArray(uid)
        parcel.writeByteArray(batchNumber)
        parcel.writeInt(productionWeek)
        parcel.writeInt(productionYear)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<VersionInfo> {
        override fun createFromParcel(parcel: Parcel): VersionInfo {
            val item = VersionInfo()
            item.hardwareVendorId = parcel.readInt()
            item.hardwareType = parcel.readInt()
            item.hardwareSubtype = parcel.readInt()
            item.hardwareVersionMajor = parcel.readInt()
            item.hardwareVersionMinor = parcel.readInt()
            item.hardwareStorageSize = parcel.readInt()
            item.hardwareProtocol = parcel.readInt()
            item.softwareVendorId = parcel.readInt()
            item.softwareType = parcel.readInt()
            item.softwareSubtype = parcel.readInt()
            item.softwareVersionMajor = parcel.readInt()
            item.softwareVersionMinor = parcel.readInt()
            item.softwareStorageSize = parcel.readInt()
            item.softwareProtocol = parcel.readInt()
            item.uid = parcel.createByteArray() ?: ByteArray(7)
            item.batchNumber = parcel.createByteArray() ?: ByteArray(5)
            item.productionWeek = parcel.readInt()
            item.productionYear = parcel.readInt()
            return item
        }

        override fun newArray(size: Int): Array<VersionInfo?> = arrayOfNulls(size)
    }
}
