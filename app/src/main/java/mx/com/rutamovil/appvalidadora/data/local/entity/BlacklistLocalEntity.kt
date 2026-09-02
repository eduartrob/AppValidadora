package mx.com.rutamovil.appvalidadora.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un bloqueo generado localmente en el dispositivo.
 * Permite mantener un registro de tarjetas bloqueadas antes de ser enviadas al servidor.
 *
 * @property id Identificador interno autogenerado.
 * @property uuid Identificador único de la tarjeta bloqueada.
 * @property number Número de referencia de la tarjeta.
 * @property timestamp Estampa de tiempo en la que se realizó el bloqueo local.
 */
@Entity(tableName = "blacklist_local")
data class BlacklistLocalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String?,
    val number: String?,
    val timestamp: Long
)
