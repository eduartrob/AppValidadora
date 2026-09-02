package mx.com.rutamovil.appvalidadora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que almacena los metadatos del último proceso de sincronización exitoso.
 * Se utiliza para llevar un control de la salud de la comunicación entre el dispositivo y el servidor.
 *
 * @property id Identificador único del registro de sincronización.
 * @property fechaSincronizacion Cadena de texto que representa el momento de la sincronización.
 * @property deviceUptimeAtSync Tiempo de actividad del hardware al sincronizar.
 * @property networkTimeAtSync Tiempo de red registrado al momento del proceso.
 * @property tipo Categoría de sincronización (ej. Transacciones, Blacklist).
 * @property transaccionesSincronizadas Número de registros afectados en el proceso.
 * @property resultado Descripción o código de éxito/error del proceso.
 */
@Entity(tableName = "ultima_sincronizacion")
data class UltimaSincronizacionEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "fecha_sincronizacion") var fechaSincronizacion: String? = null,
    @ColumnInfo(name = "device_uptime_at_sync") var deviceUptimeAtSync: Long = 0,
    @ColumnInfo(name = "network_time_at_sync") var networkTimeAtSync: Long = 0,
    var tipo: String? = null,
    @ColumnInfo(name = "transacciones_sincronizadas") var transaccionesSincronizadas: Int = 0,
    var resultado: String? = null
)
