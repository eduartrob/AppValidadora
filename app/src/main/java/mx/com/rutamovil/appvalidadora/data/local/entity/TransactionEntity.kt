package mx.com.rutamovil.appvalidadora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que registra cada evento de cobro realizado por el validador.
 * Fundamental para la auditoría y sincronización de ingresos.
 *
 * @property id Identificador interno del registro de transacción.
 * @property cardUuid Identificador de la tarjeta con la que se realizó el pago.
 * @property passengerType Tipo de tarifa aplicada (estudiante, regular, etc.).
 * @property amount Monto monetario de la transacción.
 * @property description Detalles adicionales sobre el evento de cobro.
 * @property chargedAt Fecha y hora en que se realizó el cargo localmente.
 * @property syncedAt Fecha y hora en la que se completó la sincronización con el servidor.
 * @property synced Estado de sincronización (verdadero si ya existe en el servidor).
 * @property deviceUptimeMs Tiempo de actividad del dispositivo al momento de la transacción para validación interna.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "card_uuid") var cardUuid: String? = null,
    @ColumnInfo(name = "passenger_type") var passengerType: String? = null,
    var amount: Double = 0.0,
    var description: String? = null,
    @ColumnInfo(name = "charged_at") var chargedAt: String? = null,
    @ColumnInfo(name = "synced_at") var syncedAt: String? = null,
    var synced: Boolean = false,
    @ColumnInfo(name = "device_uptime_ms") var deviceUptimeMs: Long = 0
)
