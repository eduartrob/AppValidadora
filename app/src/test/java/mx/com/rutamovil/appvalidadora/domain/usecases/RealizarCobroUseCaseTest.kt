package mx.com.rutamovil.appvalidadora.domain.usecases

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import mx.com.rutamovil.appvalidadora.data.local.AppDatabase
import mx.com.rutamovil.appvalidadora.data.local.dao.BlacklistDao
import mx.com.rutamovil.appvalidadora.data.local.dao.TransactionDao
import mx.com.rutamovil.appvalidadora.domain.helpers.ControlCortesHelper
import mx.com.rutamovil.appvalidadora.domain.repositories.INfcRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de pruebas unitarias para el caso de uso [RealizarCobroUseCase].
 * Valida los diferentes escenarios del flujo de cobro, incluyendo éxitos, 
 * errores de validación y estados de saldo.
 */
class RealizarCobroUseCaseTest {

    // Dobles de prueba (Mocks) para aislar la lógica del caso de uso.
    private val nfcRepository = mockk<INfcRepository>(relaxed = true)
    private val db = mockk<AppDatabase>(relaxed = true)
    private val blacklistDao = mockk<BlacklistDao>(relaxed = true)
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val cortesHelper = mockk<ControlCortesHelper>(relaxed = true)

    private lateinit var useCase: RealizarCobroUseCase

    /** Configuración inicial de los mocks antes de cada prueba. */
    @Before
    fun setup() {
        coEvery { db.blacklistDao() } returns blacklistDao
        coEvery { db.transactionDao() } returns transactionDao
        useCase = RealizarCobroUseCase(nfcRepository, db, cortesHelper)
    }

    /**
     * Valida que, ante condiciones ideales, el cobro se realice correctamente 
     * devolviendo el nuevo saldo y el monto debitado.
     */
    @Test
    fun `cuando el cobro es exitoso retorna el monto y el nuevo saldo`() = runBlocking {
        // Arrange: Preparación del escenario de éxito.
        val tag = Any()
        val uidHex = "ABC123"
        val aid = "123456"
        val adminKeyHex = "001122"
        val categoria = "REGULAR"
        val monto = 10.0
        val saldoInicial = 50.0
        val saldoFinal = 40.0

        coEvery { nfcRepository.connect(any()) } returns true
        coEvery { nfcRepository.authenticate(any(), any()) } returns true
        coEvery { nfcRepository.isCardBlocked() } returns false
        coEvery { blacklistDao.findByUuid(any()) } returns null
        coEvery { nfcRepository.readRole() } returns 1 // ROLE_REGULAR
        coEvery { cortesHelper.normalizar("REGULAR") } returns "regular"
        coEvery { cortesHelper.getTarifaDesdeCache(any(), any()) } returns monto
        coEvery { nfcRepository.readBalance() } returnsMany listOf(saldoInicial, saldoFinal)
        coEvery { nfcRepository.debitBalance(any()) } returns true

        // Act: Ejecución de la acción.
        val (exito, mensaje, montoCobrado) = useCase(tag, uidHex, aid, adminKeyHex, categoria, false)

        // Assert: Verificación de resultados y efectos secundarios.
        assertTrue(exito)
        assertEquals("40.0", mensaje)
        assertEquals(10.0, montoCobrado, 0.01)
        
        coVerify { nfcRepository.debitBalance(monto) }
        coVerify { transactionDao.insert(any()) }
    }

    /**
     * Valida que no se permita iniciar un cobro si no se ha seleccionado previamente una tarifa.
     */
    @Test
    fun `cuando no hay categoria seleccionada retorna error`() = runBlocking {
        // Act
        val (exito, mensaje, monto) = useCase(Any(), "UID", "AID", "KEY", null, false)

        // Assert
        assertEquals(false, exito)
        assertTrue(mensaje.contains("SELECCIONE TARIFA"))
        assertEquals(0.0, monto, 0.0)
    }

    /**
     * Valida que el sistema detenga el cobro si la tarjeta no cuenta con fondos suficientes.
     */
    @Test
    fun `cuando el saldo es insuficiente retorna error`() = runBlocking {
        // Arrange
        coEvery { nfcRepository.connect(any()) } returns true
        coEvery { nfcRepository.authenticate(any(), any()) } returns true
        coEvery { nfcRepository.isCardBlocked() } returns false
        coEvery { blacklistDao.findByUuid(any()) } returns null
        coEvery { nfcRepository.readRole() } returns 1
        coEvery { cortesHelper.normalizar(any()) } returns "regular"
        coEvery { cortesHelper.getTarifaDesdeCache(any(), any()) } returns 15.0
        coEvery { nfcRepository.readBalance() } returns 10.0

        // Act
        val (exito, mensaje, monto) = useCase(Any(), "UID", "AID", "KEY", "REGULAR", false)

        // Assert
        assertEquals(false, exito)
        assertTrue(mensaje.contains("SALDO INSUFICIENTE"))
        assertEquals(0.0, monto, 0.0)
    }
}
