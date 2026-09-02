package mx.com.rutamovil.appvalidadora.data.hardware.nfc.key

/**
 * Enumeración que identifica los algoritmos de cifrado soportados por las llaves DESFire.
 */
enum class DesfireKeyType(val id: Int) {
    /** Sin algoritmo definido. */
    NONE(0),
    /** Algoritmo Data Encryption Standard (8 bytes). */
    DES(1),
    /** Algoritmo Triple DES de 2 llaves (16 bytes). */
    TDES(2),
    /** Algoritmo Triple DES de 3 llaves (24 bytes). */
    TKTDES(3),
    /** Algoritmo Advanced Encryption Standard (16 bytes). */
    AES(4);

    companion object {
        /**
         * Obtiene el tipo de llave a partir de su identificador numérico.
         */
        fun getType(id: Int): DesfireKeyType {
            return values().find { it.id == id } ?: NONE
        }
    }
}
