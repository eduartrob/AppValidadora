package mx.com.rutamovil.appvalidadora.data.repositories

import mx.com.rutamovil.appvalidadora.data.local.dao.TarjetaDao
import mx.com.rutamovil.appvalidadora.data.local.entity.TarjetaEntity
import mx.com.rutamovil.appvalidadora.domain.model.Tarjeta
import mx.com.rutamovil.appvalidadora.domain.repositories.ICardRepository

/**
 * Implementación del repositorio de gestión de tarjetas utilizando Room para la persistencia local.
 * Actúa como puente entre los modelos de dominio y las entidades de base de datos.
 *
 * @property tarjetaDao Objeto de acceso a datos para la tabla de tarjetas.
 */
class CardRepositoryImpl(private val tarjetaDao: TarjetaDao) : ICardRepository {

    /**
     * Persiste la información de una tarjeta en el almacenamiento local.
     *
     * @param tarjeta Objeto de dominio que representa la tarjeta.
     */
    override suspend fun saveCard(tarjeta: Tarjeta) {
        tarjetaDao.insert(tarjeta.toEntity())
    }

    /**
     * Recupera una tarjeta del almacenamiento local mediante su identificador físico (UID).
     *
     * @param uid Identificador único de la tarjeta física.
     * @return Objeto de dominio [Tarjeta] o nulo si no existe en la base de datos.
     */
    override suspend fun getCardByUid(uid: String): Tarjeta? {
        val entity = tarjetaDao.getCardByUid(uid)
        return entity?.toDomain()
    }

    /**
     * Actualiza la información de una tarjeta existente en la persistencia local.
     *
     * @param tarjeta Objeto de dominio con los datos actualizados.
     */
    override suspend fun updateCard(tarjeta: Tarjeta) {
        tarjetaDao.update(tarjeta.toEntity())
    }

    /**
     * Extensión para convertir un objeto de dominio Tarjeta en una entidad de base de datos.
     *
     * @return Instancia de [TarjetaEntity].
     */
    private fun Tarjeta.toEntity() = TarjetaEntity(
        uid = this.uid,
        aid = this.aid,
        masterKey = this.masterKey,
        adminKey = this.adminKey,
        roleCode = this.roleCode,
        createdAt = this.createdAt,
        isSynced = this.isSynced
    )

    /**
     * Extensión para convertir una entidad de base de datos en un objeto de dominio Tarjeta.
     *
     * @return Instancia de [Tarjeta].
     */
    private fun TarjetaEntity.toDomain() = Tarjeta(
        uid = this.uid,
        aid = this.aid,
        masterKey = this.masterKey,
        adminKey = this.adminKey,
        roleCode = this.roleCode,
        createdAt = this.createdAt,
        isSynced = this.isSynced
    )
}
