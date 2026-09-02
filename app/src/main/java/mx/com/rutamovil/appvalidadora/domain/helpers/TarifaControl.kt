package mx.com.rutamovil.appvalidadora.domain.helpers

import java.util.Collections

/**
 * Controlador de lógica para la selección secuencial de niveles de tarifa.
 * Permite gestionar múltiples precios para un mismo tipo de pasajero (ej. rutas con escalas).
 */
class TarifaControl {
    /**
     * Representa un nivel de precio específico dentro de una categoría.
     *
     * @property id Identificador de la tarifa en el servidor.
     * @property precio Valor monetario.
     * @property nombreBackend Nombre identificador de la tarifa.
     */
    data class Nivel(
        val id: Int,
        val precio: Double,
        val nombreBackend: String
    )

    private val niveles = mutableListOf<Nivel>()
    private var indiceActual = 0

    /** Devuelve la lista actual de niveles configurados. */
    fun getListaNiveles(): List<Nivel> = niveles

    /** Limpia todos los niveles y reinicia el índice de selección. */
    fun limpiar() {
        niveles.clear()
        indiceActual = 0
    }

    /** Agrega un nuevo nivel de precio y actualiza el ordenamiento. */
    fun agregarNivel(id: Int, precio: Double, nombre: String) {
        niveles.add(Nivel(id, precio, nombre))
        ordenarNiveles()
    }

    /**
     * Ordena los niveles de mayor a menor precio y establece una tarifa "REGULAR"
     * como selección predeterminada si existe.
     */
    private fun ordenarNiveles() {
        niveles.sortByDescending { it.precio }

        if (niveles.isNotEmpty()) {
            val regularIndex = niveles.indexOfFirst { 
                it.nombreBackend.uppercase().let { name -> 
                    name.contains("REGULAR") || name.contains("NORMAL") || name.contains("ADULTO") || name.contains("GENERAL")
                } 
            }
            if (regularIndex != -1) {
                indiceActual = regularIndex
            }
        }
    }

    /** Obtiene el precio del nivel actualmente seleccionado. */
    fun getPrecioActual(): Double {
        if (niveles.isEmpty()) return 0.0
        return niveles[indiceActual].precio
    }

    /** Recupera el identificador de sistema del nivel seleccionado. */
    fun getIdActual(): Int {
        if (niveles.isEmpty()) return 0
        return niveles[indiceActual].id
    }

    /** Indica si existen múltiples niveles que justifiquen controles de cambio en la UI. */
    fun esDinamica(): Boolean = niveles.size > 1

    /** Incrementa el índice para seleccionar una tarifa de menor valor (según el orden descendente). */
    fun subirNivel() {
        if (indiceActual < niveles.size - 1) {
            indiceActual++
        }
    }

    /** Decrementa el índice para seleccionar una tarifa de mayor valor. */
    fun bajarNivel() {
        if (indiceActual > 0) {
            indiceActual--
        }
    }
}
