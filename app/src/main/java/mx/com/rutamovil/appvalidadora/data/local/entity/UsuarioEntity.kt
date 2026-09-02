package mx.com.rutamovil.appvalidadora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa al usuario autenticado en la aplicación (validador/chofer).
 * Mantiene la sesión activa y la información del perfil del trabajador.
 *
 * @property id Identificador interno autogenerado.
 * @property email Correo electrónico institucional del usuario.
 * @property password Contraseña (generalmente no persistida o almacenada como hash).
 * @property token Token de acceso (Bearer) para la comunicación con la API remota.
 * @property role Rol asignado al usuario dentro del sistema administrativo.
 * @property ultimoLogin Fecha y hora de la última sesión exitosa.
 * @property activo Estado que determina si el usuario es el actual operador del validador.
 * @property identificador Identificador único de empleado o sistema.
 * @property phone Número telefónico de contacto asociado al usuario.
 * @property unidadRuta Identificador de la unidad de transporte o ruta asignada.
 */
@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var email: String? = null,
    var password: String? = null,
    var token: String? = null,
    var role: String? = null,
    @ColumnInfo(name = "ultimo_login") var ultimoLogin: String? = null,
    var activo: Boolean = false,
    var identificador: String? = null,
    var phone: String? = null,
    @ColumnInfo(name = "unidad_ruta") var unidadRuta: String? = null
)
