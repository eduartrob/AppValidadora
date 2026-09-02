package mx.com.rutamovil.appvalidadora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad detallada que representa una tarjeta física y sus llaves criptográficas asociadas.
 * Utilizada principalmente para el manejo de tarjetas DESFire y la gestión de roles.
 *
 * @property id Identificador interno único generado automáticamente por la base de datos.
 * @property uid Identificador único de la tarjeta física.
 * @property aid Identificador de aplicación (Application ID) dentro de la tarjeta DESFire.
 * @property masterKey Llave maestra cifrada asociada a la tarjeta.
 * @property adminKey Llave de administración para la gestión de archivos y configuraciones.
 * @property roleCode Código representativo del perfil de usuario (estudiante, regular, etc.).
 * @property createdAt Estampa de tiempo de la creación o registro local de la tarjeta.
 * @property isSynced Indica si la información de la tarjeta ha sido sincronizada con el servidor remoto.
 */
@Entity(tableName = "tarjetas")
data class TarjetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "aid") val aid: String,
    @ColumnInfo(name = "master_key") val masterKey: String,
    @ColumnInfo(name = "admin_key") val adminKey: String,
    @ColumnInfo(name = "role_code") val roleCode: Byte,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "synced") val isSynced: Boolean
)
