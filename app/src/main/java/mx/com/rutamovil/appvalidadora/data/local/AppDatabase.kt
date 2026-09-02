package mx.com.rutamovil.appvalidadora.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.com.rutamovil.appvalidadora.common.util.EncryptionManager
import mx.com.rutamovil.appvalidadora.data.local.dao.*
import mx.com.rutamovil.appvalidadora.data.local.entity.*
import net.sqlcipher.database.SupportFactory

/**
 * Punto de entrada principal de la base de datos persistente de la aplicación, implementado con Room.
 * Utiliza SQLCipher para proporcionar cifrado completo de los datos almacenados en disco.
 * Centraliza el acceso a todos los objetos de acceso a datos (DAOs) del sistema.
 */
@Database(
    entities = [
        TarjetaEntity::class,
        TransactionEntity::class,
        CardEntity::class,
        UsuarioEntity::class,
        UltimaSincronizacionEntity::class,
        BlacklistEntity::class,
        BlacklistLocalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /** Proporciona acceso a las operaciones sobre tarjetas físicas. */
    abstract fun tarjetaDao(): TarjetaDao
    
    /** Proporciona acceso a las operaciones sobre transacciones de cobro. */
    abstract fun transactionDao(): TransactionDao
    
    /** Proporciona acceso a las operaciones sobre tarjetas genéricas y saldos. */
    abstract fun cardDao(): CardDao
    
    /** Proporciona acceso a las operaciones sobre la sesión y perfil del usuario. */
    abstract fun usuarioDao(): UsuarioDao
    
    /** Proporciona acceso al control de sincronización de datos. */
    abstract fun ultimaSincronizacionDao(): UltimaSincronizacionDao
    
    /** Proporciona acceso a las listas negras y bloqueos de tarjetas. */
    abstract fun blacklistDao(): BlacklistDao

    companion object {
        /** Instancia única de la base de datos siguiendo el patrón Singleton. */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia actual de la base de datos o crea una nueva utilizando cifrado.
         * Se integra con [EncryptionManager] para obtener la clave de cifrado requerida por SQLCipher.
         *
         * @param context Contexto de la aplicación.
         * @return Instancia única y configurada de [AppDatabase].
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Recuperación de la clave de cifrado persistida localmente.
                val passphrase = EncryptionManager.getOrCreateKey(context).toByteArray()
                val factory = SupportFactory(passphrase)
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tarjetas_database"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
