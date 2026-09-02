package mx.com.rutamovil.appvalidadora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import mx.com.rutamovil.appvalidadora.data.local.entity.BlacklistEntity
import mx.com.rutamovil.appvalidadora.data.local.entity.BlacklistLocalEntity

/**
 * Objeto de acceso a datos para la administración de las listas de bloqueo de tarjetas.
 * Maneja tanto la lista negra global descargada del servidor como los bloqueos generados localmente.
 */
@Dao
interface BlacklistDao {
    /**
     * Persiste masivamente una lista de tarjetas en la lista negra global.
     *
     * @param list Lista de entidades de tarjetas bloqueadas.
     */
    @Insert
    suspend fun insertAll(list: List<BlacklistEntity>)

    /**
     * Agrega una tarjeta individual a la lista negra global.
     *
     * @param card Entidad de la tarjeta a bloquear.
     */
    @Insert
    suspend fun insert(card: BlacklistEntity)

    /**
     * Registra de forma masiva bloqueos generados localmente en el dispositivo.
     *
     * @param list Lista de bloqueos locales.
     */
    @Insert
    suspend fun insertAllLocal(list: List<BlacklistLocalEntity>)

    /**
     * Registra un bloqueo individual realizado localmente.
     *
     * @param card Entidad del bloqueo local.
     */
    @Insert
    suspend fun insertLocal(card: BlacklistLocalEntity)

    /**
     * Busca si una tarjeta existe en la lista negra global.
     *
     * @param uuid Identificador único de la tarjeta a consultar.
     * @return El registro del bloqueo si existe en la lista global.
     */
    @Query("SELECT * FROM blacklist WHERE uuid = :uuid LIMIT 1")
    suspend fun findByUuid(uuid: String): BlacklistEntity?

    /**
     * Recupera el contenido íntegro de la lista negra global.
     *
     * @return Lista de todas las tarjetas bloqueadas globalmente.
     */
    @Query("SELECT * FROM blacklist")
    suspend fun getAll(): List<BlacklistEntity>

    /**
     * Busca una tarjeta dentro de los bloqueos generados exclusivamente en este dispositivo.
     *
     * @param uuid Identificador de la tarjeta.
     * @return El registro del bloqueo local si se encuentra.
     */
    @Query("SELECT * FROM blacklist_local WHERE uuid = :uuid LIMIT 1")
    suspend fun findByUuidLocal(uuid: String): BlacklistLocalEntity?

    /**
     * Obtiene el listado completo de tarjetas bloqueadas localmente.
     *
     * @return Lista de entidades de bloqueos locales.
     */
    @Query("SELECT * FROM blacklist_local")
    suspend fun getAllLocal(): List<BlacklistLocalEntity>

    /**
     * Limpia la tabla de la lista negra global.
     */
    @Query("DELETE FROM blacklist")
    suspend fun deleteAll()

    /**
     * Limpia la tabla de bloqueos generados localmente.
     */
    @Query("DELETE FROM blacklist_local")
    suspend fun deleteAllLocal()
}
