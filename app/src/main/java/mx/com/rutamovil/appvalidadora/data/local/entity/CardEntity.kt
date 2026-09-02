package mx.com.rutamovil.appvalidadora.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una tarjeta genérica en el sistema.
 * Almacena información crítica de seguridad y saldo para la validación de transacciones.
 *
 * @property uuid Identificador único universal de la tarjeta (UID).
 * @property number Número de serie o identificador visible de la tarjeta.
 * @property masterKeyPlain Llave maestra en formato de texto plano (para propósitos de depuración o migración).
 * @property systemKeyPlain Llave de sistema en formato de texto plano.
 * @property balance Saldo actual disponible en la tarjeta.
 */
@Entity(tableName = "cards", indices = [Index(value = ["uuid"], unique = true)])
data class CardEntity(
    @PrimaryKey val uuid: String,
    val number: String?,
    val masterKeyPlain: String?,
    val systemKeyPlain: String?,
    val balance: Double
)
