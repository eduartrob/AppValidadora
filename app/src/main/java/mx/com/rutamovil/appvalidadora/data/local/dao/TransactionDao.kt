package mx.com.rutamovil.appvalidadora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import mx.com.rutamovil.appvalidadora.data.local.entity.TransactionEntity

/**
 * Objeto de acceso a datos para el registro y gestión de transacciones de cobro.
 * Permite el almacenamiento de cobros fuera de línea y su posterior marcado como sincronizados.
 */
@Dao
interface TransactionDao {
    /**
     * Almacena una nueva transacción en el repositorio local.
     *
     * @param transaction Entidad que representa el evento de cobro.
     * @return Identificador de fila generado para la nueva transacción.
     */
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    /**
     * Actualiza un registro de transacción existente.
     *
     * @param transaction Entidad de transacción con los cambios aplicados.
     */
    @Update
    suspend fun update(transaction: TransactionEntity)

    /**
     * Recupera todas las transacciones que aún no han sido sincronizadas con el servidor.
     *
     * @return Lista de transacciones pendientes.
     */
    @Query("SELECT * FROM transactions WHERE synced = 0")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    /**
     * Alias para obtener las transacciones no sincronizadas.
     *
     * @return Lista de transacciones en espera de envío al servidor.
     */
    @Query("SELECT * FROM transactions WHERE synced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    /**
     * Proporciona el conteo total de transacciones que requieren sincronización.
     *
     * @return Cantidad de transacciones pendientes.
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE synced = 0")
    suspend fun countPending(): Int

    /**
     * Marca un conjunto de transacciones como sincronizadas exitosamente mediante sus identificadores.
     *
     * @param ids Lista de identificadores de las transacciones procesadas.
     */
    @Query("UPDATE transactions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)

    /**
     * Marca una transacción individual como sincronizada.
     *
     * @param transactionId Identificador de la transacción.
     */
    @Query("UPDATE transactions SET synced = 1 WHERE id = :transactionId")
    suspend fun markAsSynced(transactionId: Int)

    /**
     * Actualiza el estado de sincronización y registra la estampa de tiempo proporcionada por el servidor.
     *
     * @param id Identificador de la transacción local.
     * @param syncedAt Fecha y hora de sincronización confirmada por el servidor.
     */
    @Query("UPDATE transactions SET synced_at = :syncedAt, synced = 1 WHERE id = :id")
    suspend fun updateServerTimes(id: Int, syncedAt: String)
}
