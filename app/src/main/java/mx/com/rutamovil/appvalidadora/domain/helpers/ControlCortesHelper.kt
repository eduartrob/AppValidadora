package mx.com.rutamovil.appvalidadora.domain.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import mx.com.rutamovil.appvalidadora.data.remote.models.Fare
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper

/**
 * Asistente para la gestión del caché local de tarifas de transporte.
 * Utiliza una base de datos SQLite cifrada independiente para asegurar que las tarifas
 * estén disponibles incluso sin conexión a internet.
 *
 * @param context Contexto de la aplicación.
 */
class ControlCortesHelper private constructor(context: Context) : SQLiteOpenHelper(context, "control_cortes_encrypted.db", null, 2) {

    private val mContext = context

    companion object {
        private const val TABLE_TARIFAS = "tarifas_cache"
        private const val COL_ID = "id"
        private const val COL_ROUTE_ID = "route_fare_id"
        private const val COL_PASSENGER_TYPE = "passenger_type"
        private const val COL_PRICE = "price"
        private const val COL_FARE_NAME = "fare_name"

        @Volatile
        private var instance: ControlCortesHelper? = null

        /** Obtiene la instancia Singleton del asistente. */
        fun getInstance(context: Context): ControlCortesHelper {
            return instance ?: synchronized(this) {
                instance ?: ControlCortesHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("ControlCortesHelper", "📦 Creando tablas de cache...")
        val createTable = ("CREATE TABLE $TABLE_TARIFAS (" +
                "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_ROUTE_ID INTEGER, " +
                "$COL_PASSENGER_TYPE TEXT, " +
                "$COL_PRICE TEXT, " +
                "$COL_FARE_NAME TEXT);")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TARIFAS")
        onCreate(db)
    }

    /** Recupera la llave de cifrado global para la apertura de la base de datos. */
    private fun getEncryptionKey(): String {
        return mx.com.rutamovil.appvalidadora.common.util.EncryptionManager.getOrCreateKey(mContext)
    }

    /** Abre la base de datos en modo escritura utilizando la clave de seguridad. */
    fun getWritableDatabase(): SQLiteDatabase {
        return super.getWritableDatabase(getEncryptionKey())
    }

    /** Abre la base de datos en modo lectura utilizando la clave de seguridad. */
    fun getReadableDatabase(): SQLiteDatabase {
        return super.getReadableDatabase(getEncryptionKey())
    }

    // ========== MÉTODOS PARA TARIFAS CACHE ==========

    /**
     * Almacena una tarifa en el repositorio local.
     */
    fun guardarTarifaCache(id: Int, passengerType: String?, price: String?, fareName: String?) {
        var db: SQLiteDatabase? = null
        try {
            db = getWritableDatabase()
            val values = ContentValues().apply {
                put(COL_ROUTE_ID, id)
                put(COL_PASSENGER_TYPE, passengerType)
                put(COL_PRICE, price)
                put(COL_FARE_NAME, fareName)
            }
            db.insert(TABLE_TARIFAS, null, values)
            Log.d("DEBUG", "Tarifa guardada en BD: $passengerType = $$price")
        } catch (e: Exception) {
            Log.e("ControlCortesHelper", "Error guardando tarifa: ${e.message}")
        } finally {
            db?.close()
        }
    }

    /**
     * Recupera el listado completo de tarifas almacenadas localmente.
     *
     * @return Lista de objetos de tipo [Fare].
     */
    fun obtenerTarifasCache(): List<Fare> {
        val lista = mutableListOf<Fare>()
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        try {
            db = getReadableDatabase()
            cursor = db.rawQuery("SELECT * FROM $TABLE_TARIFAS", null)
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ROUTE_ID))
                    val type = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSENGER_TYPE))
                    val price = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRICE))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_FARE_NAME))
                    lista.add(Fare(id, name, type, price))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("ControlCortesHelper", "Error obteniendo tarifas: ${e.message}")
        } finally {
            cursor?.close()
            db?.close()
        }
        return lista
    }

    /** Borra todas las tarifas del caché. */
    fun limpiarTarifasCache() {
        var db: SQLiteDatabase? = null
        try {
            db = getWritableDatabase()
            db.delete(TABLE_TARIFAS, null, null)
        } catch (e: Exception) {
            Log.e("ControlCortesHelper", "Error limpiando cache: ${e.message}")
        } finally {
            db?.close()
        }
    }

    /**
     * Busca la tarifa monetaria específica para una categoría y modalidad (mínima/máxima).
     * Implementa lógica de filtrado y ordenamiento sobre el caché local.
     *
     * @param categoria Nombre de la categoría a consultar.
     * @param esMinimo Verdadero si se desea el precio más bajo registrado para esa categoría.
     * @return Monto de la tarifa o 0.0 si no se encuentra.
     */
    fun getTarifaDesdeCache(categoria: String, esMinimo: Boolean): Double {
        val filtered = getTarifasPorCategoria(categoria)

        return if (filtered.isEmpty()) {
            0.0
        } else if (esMinimo) {
            filtered.last().price?.toDoubleOrNull() ?: 0.0
        } else {
            filtered.first().price?.toDoubleOrNull() ?: 0.0
        }
    }

    /**
     * Obtiene todas las tarifas asociadas a una categoría específica.
     *
     * @param categoria Nombre de la categoría a consultar.
     * @return Lista de tarifas encontradas.
     */
    fun getTarifasPorCategoria(categoria: String): List<Fare> {
        val tarifas = obtenerTarifasCache()
        val catNorm = normalizar(categoria)
        
        return tarifas.filter { fare ->
            val fareTypeNorm = normalizar(fare.passenger_type ?: "")
            fareTypeNorm == catNorm
        }
        .distinctBy { it.price } // Evita duplicar botones con el mismo precio
        .sortedByDescending { it.price?.toDoubleOrNull() ?: 0.0 }
    }

    /**
     * Normaliza los nombres de categorías provenientes del servidor para que coincidan
     * con las constantes lógicas de la aplicación.
     */
    fun normalizar(txt: String): String {
        val s = txt.uppercase().trim()
        return when {
            s.contains("REGULAR") || s.contains("ADULTO") || s.contains("GENERAL") -> "REGULAR"
            s.contains("ESTUD") || s.contains("UNIV") || s.contains("ESCOLAR") -> "ESTUDIANTE"
            s.contains("3") || s.contains("TERCERA") || s.contains("MAYOR") || s.contains("INAPAM") || s.contains("EDAD") -> "3ERA EDAD"
            s.contains("DISCA") || s.contains("PCD") -> "DISCAPACITADO"
            else -> s
        }
    }
}
