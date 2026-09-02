package mx.com.rutamovil.appvalidadora.data.hardware.nfc.file

/**
 * Enumeración que define los niveles de seguridad y cifrado para la comunicación con archivos DESFire.
 */
enum class DesfireFileCommunicationSettings(val value: Int, val description: String) {
    /** Comunicación en texto plano sin medidas de seguridad adicionales. */
    PLAIN(0x00, "Plain communication"),
    /** Comunicación en texto plano con un código MAC para asegurar la integridad. */
    PLAIN_MAC(0x01, "Plain communication secured by MACing"),
    /** Comunicación totalmente cifrada para garantizar confidencialidad e integridad. */
    ENCIPHERED(0x03, "Fully enciphered communication");

    companion object {
        /**
         * Parsea un valor entero devuelto por la tarjeta a su representación de ajustes de comunicación.
         */
        fun parse(value: Int): DesfireFileCommunicationSettings {
            return values().find { it.value == value }
                ?: throw IllegalArgumentException("Unknown communications settings $value")
        }
    }
}
