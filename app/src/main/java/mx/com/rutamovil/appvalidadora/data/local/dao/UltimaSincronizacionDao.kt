package mx.com.rutamovil.appvalidadora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import mx.com.rutamovil.appvalidadora.data.local.entity.UltimaSincronizacionEntity

/**
 * Objeto de acceso a datos para el control de los registros de sincronización de datos.
 * Ayuda a mantener un historial o estado único del último proceso de enlace con el servidor.
 */
@Dao
interface UltimaSincronizacionDao {
    /**
     * Registra un nuevo evento de sincronización.
     *
     * @param sincronizacion Entidad con los metadatos del proceso.
     * @return Identificador generado para el registro.
     */
    @Insert
    suspend fun insert(sincronizacion: UltimaSincronizacionEntity): Long

    /**
     * Modifica un registro de sincronización existente.
     *
     * @param sincronizacion Entidad con la información actualizada.
     */
    @Update
    suspend fun update(sincronizacion: UltimaSincronizacionEntity)

    /**
     * Obtiene el registro de sincronización más reciente almacenado en el sistema.
     *
     * @return La última entidad de sincronización o nulo si no hay registros.
     */
    @Query("SELECT * FROM ultima_sincronizacion ORDER BY id DESC LIMIT 1")
    suspend fun getRegistroUnico(): UltimaSincronizacionEntity?

    /**
     * Cuenta el número total de registros de sincronización existentes.
     *
     * @return Cantidad total de registros.
     */
    @Query("SELECT COUNT(*) FROM ultima_sincronizacion")
    suspend fun count(): Int

    /**
     * Elimina todos los registros de sincronización excepto el más reciente, optimizando el espacio.
     */
    @Query("DELETE FROM ultima_sincronizacion WHERE id NOT IN (SELECT id FROM ultima_sincronizacion ORDER BY id DESC LIMIT 1)")
    suspend fun limpiarRegistrosDuplicados()

    /**
     * Borra la totalidad de los registros de sincronización de la base de datos.
     */
    @Query("DELETE FROM ultima_sincronizacion")
    suspend fun deleteAll()
}
