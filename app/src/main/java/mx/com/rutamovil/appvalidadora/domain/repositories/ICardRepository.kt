package mx.com.rutamovil.appvalidadora.domain.repositories

import mx.com.rutamovil.appvalidadora.domain.model.Tarjeta

/**
 * Interfaz para la gestión persistente de la información de tarjetas.
 * Permite el almacenamiento y recuperación de perfiles de tarjeta locales.
 */
interface ICardRepository {
    /** Persiste un objeto de tarjeta. */
    suspend fun saveCard(tarjeta: Tarjeta)
    /** Obtiene la tarjeta asociada a un UID específico. */
    suspend fun getCardByUid(uid: String): Tarjeta?
    /** Actualiza la información de una tarjeta existente. */
    suspend fun updateCard(tarjeta: Tarjeta)
}
