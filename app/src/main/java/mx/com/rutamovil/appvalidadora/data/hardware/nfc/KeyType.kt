package mx.com.rutamovil.appvalidadora.data.hardware.nfc

/**
 * Enumeración que define los algoritmos de cifrado base soportados para las operaciones 
 * de bajo nivel con el chip DESFire.
 */
enum class KeyType {
    /** Algoritmo Data Encryption Standard simple (8 bytes). */
    DES,
    /** Algoritmo Triple DES de dos llaves (16 bytes). */
    TDES,
    /** Algoritmo Triple DES de tres llaves (24 bytes). */
    TKTDES,
    /** Algoritmo Advanced Encryption Standard (16 bytes). */
    AES;
}
