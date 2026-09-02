package mx.com.rutamovil.appvalidadora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import mx.com.rutamovil.appvalidadora.data.local.entity.CardEntity

/**
 * Interfaz de acceso a datos para la gestión de tarjetas genéricas en la base de datos local.
 * Proporciona métodos para la persistencia masiva y búsqueda individual por identificador.
 */
@Dao
interface CardDao {
    /**
     * Inserta una lista de tarjetas, reemplazando aquellas que ya existan por su identificador único.
     *
     * @param cards Lista de entidades de tarjeta a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)

    /**
     * Actualiza los datos de una tarjeta existente.
     *
     * @param card Entidad de tarjeta con los datos actualizados.
     */
    @Update
    suspend fun update(card: CardEntity)

    /**
     * Busca una tarjeta en el repositorio local mediante su identificador único.
     *
     * @param uuid Identificador único universal de la tarjeta.
     * @return La entidad de la tarjeta si se encuentra, nulo en caso contrario.
     */
    @Query("SELECT * FROM cards WHERE uuid = :uuid LIMIT 1")
    suspend fun findByUuid(uuid: String): CardEntity?

    /**
     * Recupera el listado completo de tarjetas almacenadas localmente.
     *
     * @return Lista de todas las entidades de tarjeta.
     */
    @Query("SELECT * FROM cards")
    suspend fun getAll(): List<CardEntity>

    /**
     * Elimina todos los registros de tarjetas de la base de datos.
     */
    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}
