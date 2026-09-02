package mx.com.rutamovil.appvalidadora.data.remote.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo para recibir la confirmación de la última fecha de sincronización desde el servidor.
 *
 * @property status Indica el éxito de la consulta.
 * @property data Objeto que contiene la fecha de sincronización.
 */
data class LastSyncResponse(
    val status: Boolean,
    val data: LastSyncData?
) {
    /**
     * Datos que envuelven la estampa de tiempo de la sincronización.
     *
     * @property lastSyncAt Cadena que representa la fecha y hora de la última interacción exitosa.
     */
    data class LastSyncData(
        @SerializedName("last_sync_at") val lastSyncAt: String?
    )
}
