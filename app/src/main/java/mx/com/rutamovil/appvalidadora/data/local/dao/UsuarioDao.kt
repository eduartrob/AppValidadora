package mx.com.rutamovil.appvalidadora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import mx.com.rutamovil.appvalidadora.data.local.entity.UsuarioEntity

/**
 * Objeto de acceso a datos para la gestión de la sesión y perfil del usuario local.
 * Permite controlar el estado de autenticación y la información del operador del dispositivo.
 */
@Dao
interface UsuarioDao {
    /**
     * Inserta un nuevo registro de usuario en la base de datos.
     *
     * @param usuario Entidad que contiene la información del perfil de usuario.
     * @return El identificador de fila generado para la inserción.
     */
    @Insert
    suspend fun insert(usuario: UsuarioEntity): Long

    /**
     * Actualiza la información del perfil de un usuario existente.
     *
     * @param usuario Entidad de usuario con los datos actualizados.
     */
    @Update
    suspend fun update(usuario: UsuarioEntity)

    /**
     * Recupera un usuario basándose en su correo electrónico, siempre que su sesión esté marcada como activa.
     *
     * @param email Correo electrónico del usuario.
     * @return La entidad del usuario si se encuentra activo, nulo de lo contrario.
     */
    @Query("SELECT * FROM usuario WHERE email = :email AND activo = 1 LIMIT 1")
    suspend fun getUsuarioByEmail(email: String): UsuarioEntity?

    /**
     * Busca un usuario activo mediante su token de acceso Bearer.
     *
     * @param token Token de autenticación del usuario.
     * @return El usuario asociado al token si está marcado como activo.
     */
    @Query("SELECT * FROM usuario WHERE token = :token AND activo = 1 LIMIT 1")
    suspend fun getUsuarioByToken(token: String): UsuarioEntity?

    /**
     * Obtiene el usuario que actualmente tiene la sesión activa en el validador.
     *
     * @return Entidad del usuario activo o nulo si no hay sesiones abiertas.
     */
    @Query("SELECT * FROM usuario WHERE activo = 1 LIMIT 1")
    suspend fun getUsuarioActivo(): UsuarioEntity?

    /**
     * Marca a un usuario como inactivo, finalizando efectivamente su sesión local.
     *
     * @param id Identificador interno del usuario a desactivar.
     */
    @Query("UPDATE usuario SET activo = 0 WHERE id = :id")
    suspend fun desactivarUsuario(id: Int)

    /**
     * Actualiza el token de autenticación para un usuario identificado por su correo electrónico.
     *
     * @param email Correo electrónico del usuario.
     * @param newToken El nuevo token de acceso proporcionado por el servidor.
     */
    @Query("UPDATE usuario SET token = :newToken WHERE email = :email")
    suspend fun actualizarToken(email: String, newToken: String)

    /**
     * Elimina todos los registros de usuarios de la persistencia local.
     */
    @Query("DELETE FROM usuario")
    suspend fun deleteAll()
}
