package mx.com.rutamovil.appvalidadora.data.hardware.nfc.file

/**
 * Enumeración que define los diferentes tipos de archivos soportados por la arquitectura MIFARE DESFire.
 */
enum class DesfireFileType(val id: Int) {
    /** Archivo de datos estándar sin soporte de transacciones (Backup). */
    STANDARD_DATA_FILE(0x00),
    /** Archivo de datos con soporte de copia de seguridad (Commit/Abort). */
    BACKUP_DATA_FILE(0x01),
    /** Archivo especializado para el almacenamiento de valores numéricos (Monedero). */
    VALUE_FILE(0x02),
    /** Archivo estructurado en registros de tamaño fijo. */
    LINEAR_RECORD_FILE(0x03),
    /** Archivo de registros que sobrescribe el más antiguo al llenarse. */
    CYCLIC_RECORD_FILE(0x04),
    /** Tipo de archivo no reconocido o no soportado. */
    UNKNOWN_FILE_TYPE(0xFF);

    companion object {
        /**
         * Obtiene el tipo de archivo correspondiente a un identificador numérico.
         */
        fun getType(id: Int): DesfireFileType {
            return values().find { it.id == id } ?: UNKNOWN_FILE_TYPE
        }
    }
}
