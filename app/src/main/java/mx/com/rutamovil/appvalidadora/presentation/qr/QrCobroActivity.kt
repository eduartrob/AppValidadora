package mx.com.rutamovil.appvalidadora.presentation.qr

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import mx.com.rutamovil.appvalidadora.R
import java.util.Locale

/**
 * Actividad encargada de gestionar el proceso de cobro mediante la lectura de códigos QR.
 * Proporciona una interfaz visual para la captura de la cámara y la retroalimentación 
 * del estado de la transacción QR.
 */
class QrCobroActivity : AppCompatActivity() {

    private lateinit var mainContent: ConstraintLayout
    private lateinit var successOverlay: ConstraintLayout
    private lateinit var errorOverlay: ConstraintLayout
    private lateinit var warningOverlay: ConstraintLayout

    private lateinit var txtMonto: TextView
    private lateinit var txtSaldo: TextView
    private lateinit var txtErrorTitle: TextView
    private lateinit var txtErrorMsg: TextView

    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_cobro)

        inicializarVistas()
        configurarListeners()

        // Generador de tonos para retroalimentación auditiva tras el escaneo.
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        iniciarLectorQR()
    }

    /** Vincula las vistas de la actividad con sus referencias en código. */
    private fun inicializarVistas() {
        mainContent = findViewById(R.id.mainContent)
        successOverlay = findViewById(R.id.successOverlay)
        errorOverlay = findViewById(R.id.errorOverlay)
        warningOverlay = findViewById(R.id.warningOverlay)

        txtMonto = findViewById(R.id.txtMonto)
        txtSaldo = findViewById(R.id.txtSaldo)
        txtErrorTitle = findViewById(R.id.txtErrorTitle)
        txtErrorMsg = findViewById(R.id.txtErrorMsg)
    }

    /** Configura los eventos de clic para botones de navegación. */
    private fun configurarListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnCancel).setOnClickListener { finish() }
    }

    /** Inicializa los recursos de la cámara para la detección de códigos QR. */
    private fun iniciarLectorQR() {
        // Implementación pendiente: Integrar librería de escaneo (CameraX o ZXing).
    }

    /**
     * Muestra la interfaz de éxito tras un pago QR confirmado.
     *
     * @param monto Cantidad debitada.
     * @param saldoRestante Balance actual de la billetera virtual del usuario.
     */
    fun mostrarExito(monto: Double, saldoRestante: Double) {
        runOnUiThread {
            mainContent.visibility = View.GONE
            successOverlay.visibility = View.VISIBLE

            txtMonto.text = String.format(Locale.US, "$%.2f", monto)
            txtSaldo.text = String.format(Locale.US, "SALDO ACTUAL: $%.2f", saldoRestante)

            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
            // Cierre automático de la pantalla tras 3 segundos de éxito.
            handler.postDelayed({ finish() }, 3000)
        }
    }

    /**
     * Muestra la interfaz de error ante fallos en la transacción QR.
     *
     * @param titulo Encabezado del error.
     * @param mensaje Detalle explicativo para el usuario.
     */
    fun mostrarError(titulo: String, mensaje: String) {
        runOnUiThread {
            mainContent.visibility = View.GONE
            errorOverlay.visibility = View.VISIBLE

            txtErrorTitle.text = titulo
            txtErrorMsg.text = mensaje

            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ANSWER, 500)
            handler.postDelayed({ finish() }, 4000)
        }
    }

    /** Indica al usuario que el código no pudo ser interpretado correctamente. */
    fun mostrarFalloLectura() {
        runOnUiThread {
            mainContent.visibility = View.GONE
            warningOverlay.visibility = View.VISIBLE

            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 150)

            handler.postDelayed({
                warningOverlay.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
            }, 3000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
