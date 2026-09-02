package mx.com.rutamovil.appvalidadora.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una tarjeta en la lista negra global del sistema.
 * Se utiliza para denegar el acceso o cobro a tarjetas reportadas o inválidas.
 *
 * @property uuid Identificador único (UID) de la tarjeta bloqueada.
 * @property number Número visible o de referencia de la tarjeta.
 */
@Entity(tableName = "blacklist")
data class BlacklistEntity(
    @PrimaryKey val uuid: String,
    val number: String?
)
