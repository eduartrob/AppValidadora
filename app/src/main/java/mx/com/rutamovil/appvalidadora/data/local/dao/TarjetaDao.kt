package mx.com.rutamovil.appvalidadora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import mx.com.rutamovil.appvalidadora.data.local.entity.TarjetaEntity

/**
 * Objeto de acceso a datos para la gestión de tarjetas físicas y sus configuraciones criptográficas.
 * Centraliza la persistencia de los perfiles de tarjeta utilizados en el proceso de validación.
 */
@Dao
interface TarjetaDao {
    /**
     * Registra una nueva tarjeta en la base de datos local.
     *
     * @param tarjeta Entidad con la información técnica de la tarjeta.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tarjeta: TarjetaEntity)

    /**
     * Actualiza la información de una tarjeta registrada previamente.
     *
     * @param tarjeta Entidad de tarjeta con la información modificada.
     */
    @Update
    suspend fun update(tarjeta: TarjetaEntity)

    /**
     * Obtiene una tarjeta a través de su identificador físico (UID).
     *
     * @param uid Identificador único de la tarjeta física.
     * @return La entidad correspondiente o nulo si no existe el registro.
     */
    @Query("SELECT * FROM tarjetas WHERE uid = :uid LIMIT 1")
    suspend fun getCardByUid(uid: String): TarjetaEntity?

    /**
     * Recupera todas las tarjetas registradas localmente.
     *
     * @return Lista de entidades de tarjeta.
     */
    @Query("SELECT * FROM tarjetas")
    suspend fun getAll(): List<TarjetaEntity>

    /**
     * Cuenta la existencia de una tarjeta específica por su UID.
     *
     * @param uid Identificador único de la tarjeta.
     * @return 1 si existe, 0 en caso contrario.
     */
    @Query("SELECT COUNT(*) FROM tarjetas WHERE uid = :uid")
    suspend fun countCardByUid(uid: String): Int

    /**
     * Elimina el registro de una tarjeta basándose en su identificador físico.
     *
     * @param uid Identificador único de la tarjeta a eliminar.
     */
    @Query("DELETE FROM tarjetas WHERE uid = :uid")
    suspend fun deleteCardByUid(uid: String)

    /**
     * Marca un registro de tarjeta como sincronizado con el servidor.
     *
     * @param uid Identificador único de la tarjeta sincronizada.
     */
    @Query("UPDATE tarjetas SET synced = 1 WHERE uid = :uid")
    suspend fun markAsSyncedByUid(uid: String)

    /**
     * Obtiene el listado de tarjetas que aún no han sido sincronizadas con el servidor remoto.
     *
     * @return Lista de tarjetas con estado de sincronización pendiente.
     */
    @Query("SELECT * FROM tarjetas WHERE synced = 0")
    suspend fun getPendingSync(): List<TarjetaEntity>
}
