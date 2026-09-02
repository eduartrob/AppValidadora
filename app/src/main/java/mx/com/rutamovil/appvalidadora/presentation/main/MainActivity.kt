package mx.com.rutamovil.appvalidadora.presentation.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.com.rutamovil.appvalidadora.R
import mx.com.rutamovil.appvalidadora.common.util.Utils
import mx.com.rutamovil.appvalidadora.data.remote.ApiService
import mx.com.rutamovil.appvalidadora.data.remote.models.Fare
import mx.com.rutamovil.appvalidadora.domain.helpers.CobroBotones
import mx.com.rutamovil.appvalidadora.domain.helpers.ControlCortesHelper
import mx.com.rutamovil.appvalidadora.presentation.ViewModelFactory
import mx.com.rutamovil.appvalidadora.presentation.login.LoginActivity
import mx.com.rutamovil.appvalidadora.presentation.qr.QrCobroActivity

/**
 * Actividad principal del validador.
 * Gestiona la interfaz de cobro NFC, el monitoreo del estado del dispositivo (batería, WiFi, GPS)
 * y la navegación hacia otras funcionalidades como el cobro por QR.
 */
class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CobroBotones.CobroCallback {

    private lateinit var viewModel: MainViewModel
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var cobroBotones: CobroBotones
    private var fareDialog: AlertDialog? = null

    // Vistas del layout principal
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mainContent: View
    private lateinit var tvStatus: TextView
    private lateinit var txtFooterTarifa: TextView

    // Overlays de resultado de operación
    private lateinit var successOverlay: View
    private lateinit var errorOverlay: View
    private lateinit var txtSuccessOverlay: TextView
    private lateinit var txtSuccessSaldo: TextView
    private lateinit var txtMontoPagado: TextView
    private lateinit var txtErrorOverlay: TextView
    private lateinit var successImage: ImageView
    private lateinit var errorImage: ImageView

    // Vistas informativas del menú lateral (Drawer)
    private lateinit var txtWifiStatus: TextView
    private lateinit var txtGpsStatus: TextView
    private lateinit var txtBatteryMenu: TextView
    private lateinit var imgWifiMenu: ImageView

    /** Receptor de cambios en el estado de la batería. */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                actualizarUIBateria(batteryPct)
            }
        }
    }

    /** Receptor de cambios en los proveedores de ubicación (GPS). */
    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                actualizarUIGps()
            }
        }
    }

    /** Callback para monitorear la conectividad a la red WiFi. */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread { 
                actualizarUIWifi(true)
                descargarTarifasDelServidor()
            }
        }

        override fun onLost(network: Network) {
            runOnUiThread { actualizarUIWifi(false) }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            runOnUiThread { actualizarUIWifi(isWifi) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configuración del token de sesión para peticiones de red.
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)
        Log.d("MainActivity", "Token recuperado de prefs: ${token?.take(20)}...")
        ApiService.setValidatorToken(token)

        val factory = ViewModelFactory(this.applicationContext)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Validación de disponibilidad de hardware NFC.
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC no soportado en este dispositivo", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        inicializarVistas()
        configurarMenuLateral()
        observarEstados()

        cobroBotones = CobroBotones(this)
        cargarTarifas()
    }

    /** Procesa las teclas físicas del validador para la selección de tarifas. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (cobroBotones.procesarTecla(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /** Vincula los componentes de la vista e inicializa listeners de navegación. */
    private fun inicializarVistas() {
        drawerLayout = findViewById(R.id.drawerLayout)
        mainContent = findViewById(R.id.mainContent)
        tvStatus = findViewById(R.id.txtAproximeChip)
        txtFooterTarifa = findViewById(R.id.txtFooterTarifa)

        successOverlay = findViewById(R.id.successOverlay)
        errorOverlay = findViewById(R.id.errorOverlay)
        txtSuccessOverlay = findViewById(R.id.txtSuccessOverlay)
        txtSuccessSaldo = findViewById(R.id.txtSuccessSaldo)
        txtMontoPagado = findViewById(R.id.txtMontoPagado)
        txtErrorOverlay = findViewById(R.id.txtErrorOverlay)
        successImage = findViewById(R.id.successImage)
        errorImage = findViewById(R.id.errorImage)

        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btnCobroQr).setOnClickListener {
            val intent = Intent(this, QrCobroActivity::class.java)
            startActivity(intent)
        }
    }

    /** Configura las opciones y monitores del panel de administración lateral. */
    private fun configurarMenuLateral() {
        val btnLogout = findViewById<Button>(R.id.btnMenuLogout)
        txtWifiStatus = findViewById(R.id.txtWifiStatus)
        txtGpsStatus = findViewById(R.id.txtGpsStatus)
        txtBatteryMenu = findViewById(R.id.txtBatteryMenu)
        imgWifiMenu = findViewById(R.id.imgWifiMenu)

        btnLogout.setOnClickListener {
            confirmarCerrarSesion()
        }

        actualizarEstadosMenuLateral()
    }

    /** Muestra un diálogo de confirmación para finalizar la sesión del operador. */
    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Está seguro que desea salir del sistema?")
            .setPositiveButton("SÍ, SALIR") { _, _ ->
                ejecutarLogout()
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    /** Limpia los datos de sesión y redirige a la pantalla de inicio de sesión. */
    private fun ejecutarLogout() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        val loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        loginPrefs.edit().clear().apply()

        ApiService.setValidatorToken(null)

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
        
        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
    }

    /** Actualiza los indicadores de salud del hardware en el menú lateral. */
    private fun actualizarEstadosMenuLateral() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        actualizarUIWifi(isWifi)
        actualizarUIGps()
    }

    private fun actualizarUIWifi(isOk: Boolean) {
        if (isOk) {
            txtWifiStatus.text = "OK"
            txtWifiStatus.setTextColor(Color.WHITE)
        } else {
            txtWifiStatus.text = "NO"
            txtWifiStatus.setTextColor(ContextCompat.getColor(this, R.color.colorError))
        }
    }

    private fun actualizarUIGps() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGpsEnabled) {
            txtGpsStatus.text = "OK"
            txtGpsStatus.setTextColor(Color.WHITE)
        } else {
            txtGpsStatus.text = "NO"
            txtGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.colorError))
        }
    }

    private fun actualizarUIBateria(porcentaje: Int) {
        txtBatteryMenu.text = "$porcentaje%"
        if (porcentaje <= 15) {
            txtBatteryMenu.setTextColor(ContextCompat.getColor(this, R.color.colorError))
        } else {
            txtBatteryMenu.setTextColor(Color.WHITE)
        }
    }

    /** Callback del gestor de botones indicando que se ha seleccionado una tarifa. */
    override fun onEsperandoTarjeta(mensaje: String) {
        runOnUiThread {
            txtFooterTarifa.text = mensaje
            txtFooterTarifa.setTextColor(Color.parseColor("#FFB300")) 
        }
    }

    /** Callback del gestor de botones para restablecer la selección de tarifa. */
    override fun onResetTarifa() {
        runOnUiThread {
            txtFooterTarifa.text = "Tarifa: por rol"
            txtFooterTarifa.setTextColor(Color.WHITE)
        }
    }

    /** Inicia la carga de tarifas prefiriendo el servidor sobre el caché local. */
    private fun cargarTarifas() {
        if (hayInternet()) {
            descargarTarifasDelServidor()
        } else {
            cargarTarifasDesdeCacheLocal()
        }
    }

    /** Descarga el catálogo de tarifas actual e invalida el caché previo. */
    private fun descargarTarifasDelServidor() {
        ApiService.obtenerTarifas(object : ApiService.ApiCallback<List<Fare>> {
            override fun onSuccess(tarifas: List<Fare>) {
                if (tarifas.isEmpty()) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "⚠️ El servidor no envió tarifas", Toast.LENGTH_SHORT).show() }
                    return
                }

                val cortesHelper = ControlCortesHelper.getInstance(this@MainActivity)
                cortesHelper.limpiarTarifasCache()
                for (fare in tarifas) {
                    cortesHelper.guardarTarifaCache(
                        fare.id,
                        fare.passenger_type,
                        fare.price,
                        fare.fare
                    )
                }
                runOnUiThread {
                    val resumen = tarifas.groupBy { it.passenger_type }.map { "${it.key}: ${it.value.size}" }.joinToString(", ")
                    Log.d("DEBUG", "Tarifas en BD: $resumen")
                    Toast.makeText(this@MainActivity, "✅ Tarifas actualizadas: $resumen", Toast.LENGTH_LONG).show()
                    
                    viewModel.cargarTarifasDesdeBD()
                    txtFooterTarifa.text = "Tarifa: por rol"
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    Log.e("API_TARIFAS", "Error: $error")
                    cargarTarifasDesdeCacheLocal()
                }
            }
        })
    }

    /** Recupera las tarifas desde el repositorio local en caso de estar fuera de línea. */
    private fun cargarTarifasDesdeCacheLocal() {
        val cortesHelper = ControlCortesHelper.getInstance(this)
        val tarifas = cortesHelper.obtenerTarifasCache()
        if (tarifas.isEmpty()) {
            txtFooterTarifa.text = "⚠️ Sin tarifas cargadas"
            txtFooterTarifa.setTextColor(Color.parseColor("#FFD600"))
            Toast.makeText(this, "⚠️ ADVERTENCIA: Base de datos de precios vacía", Toast.LENGTH_LONG).show()
        } else {
            txtFooterTarifa.text = "Tarifa: por rol (Offline)"
            txtFooterTarifa.setTextColor(Color.WHITE)
            Log.d("DEBUG", "Tarifas cargadas de BD local: ${tarifas.size}")
        }
    }

    /** Verifica la disponibilidad de acceso a internet. */
    private fun hayInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /** Suscribe la vista a los flujos de datos y estados del ViewModel. */
    private fun observarEstados() {
        lifecycleScope.launchWhenStarted {
            viewModel.showFareSelection.collect { data ->
                if (data != null) {
                    mostrarDialogoSeleccionTarifa(data)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.tarifaNombre.collect { nombre ->
                txtFooterTarifa.text = nombre
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ValidadorState.Listo -> {
                        tvStatus.text = "Aproxime la tarjeta para cobrar"
                        tvStatus.setTextColor(Color.DKGRAY)
                    }
                    is ValidadorState.Procesando -> {
                        tvStatus.text = "Procesando cobro... NO MUEVA LA TARJETA"
                        tvStatus.setTextColor(Color.parseColor("#FFD600"))
                    }
                    is ValidadorState.Exito -> {
                        cobroBotones.limpiarSeleccion()
                        mostrarExito("PAGO EXITOSO", state.saldoRestante)
                    }
                    is ValidadorState.Error -> {
                        mostrarError(state.mensaje)
                    }
                }
            }
        }
    }

    /** Muestra la pantalla de éxito con animación y confirmación sonora. */
    private fun mostrarExito(mensaje: String, saldo: String) {
        successOverlay.visibility = View.VISIBLE
        mainContent.visibility = View.GONE
        txtSuccessOverlay.text = mensaje
        txtSuccessSaldo.text = "SALDO ACTUAL: $$saldo"
        
        txtMontoPagado.text = "$${String.format("%.2f", viewModel.tarifaActual.value)}"

        successImage.scaleX = 0f
        successImage.scaleY = 0f
        successImage.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        emitirSonido(true)
        programarResetPantalla(2000)
    }

    /** Muestra la pantalla de error con el motivo del fallo y señal auditiva. */
    private fun mostrarError(mensaje: String) {
        errorOverlay.visibility = View.VISIBLE
        mainContent.visibility = View.GONE
        txtErrorOverlay.text = mensaje

        errorImage.scaleX = 0f
        errorImage.scaleY = 0f
        errorImage.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        emitirSonido(false)
        programarResetPantalla(3000)
    }

    /** Genera tonos auditivos para retroalimentación táctil del operador. */
    private fun emitirSonido(exito: Boolean) {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            if (exito) {
                toneG.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            } else {
                toneG.startTone(ToneGenerator.TONE_PROP_NACK, 500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Temporiza el regreso a la pantalla principal después de mostrar un resultado. */
    private fun programarResetPantalla(delayMs: Long) {
        lifecycleScope.launch {
            delay(delayMs)
            runOnUiThread {
                successOverlay.visibility = View.GONE
                errorOverlay.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
                viewModel.resetState()
                fareDialog?.dismiss()
                fareDialog = null
            }
        }
    }

    /** Muestra el diálogo para la selección manual de precios tras detectar la tarjeta. */
    private fun mostrarDialogoSeleccionTarifa(data: FareSelectionData) {
        if (fareDialog != null) return

        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_fare_selection, null)
        builder.setView(dialogView)
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val txtTitle = dialogView.findViewById<TextView>(R.id.txtDialogTitle)
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.containerFares)
        val btnCancelTop = dialogView.findViewById<ImageButton>(R.id.btnCancelDialog)
        val btnCancelBottom = dialogView.findViewById<Button>(R.id.btnFooterCancel)

        txtTitle.text = "TARIFA ${data.roleName}"

        // Añadir dinámicamente las opciones de precios
        data.fares.forEachIndexed { index, fare ->
            val rowView = layoutInflater.inflate(R.layout.item_fare_option, container, false)
            val txtName = rowView.findViewById<TextView>(R.id.txtFareName)
            val txtPrice = rowView.findViewById<TextView>(R.id.txtFarePrice)

            txtName.text = fare.fare
            txtPrice.text = "$${fare.price}"

            // Añadir separador visual si no es el último elemento
            if (index < data.fares.size - 1) {
                val divider = View(this)
                divider.layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    Utils.dpToPx(this, 1)
                )
                divider.setBackgroundColor(Color.parseColor("#43A047")) // Verde más claro para el separador
                
                rowView.setOnClickListener {
                    viewModel.confirmarCobroManual(data, fare.price?.toDoubleOrNull() ?: 0.0, fare.fare ?: "DESCONOCIDO")
                    dialog.dismiss()
                    fareDialog = null
                }
                container.addView(rowView)
                container.addView(divider)
            } else {
                rowView.setOnClickListener {
                    viewModel.confirmarCobroManual(data, fare.price?.toDoubleOrNull() ?: 0.0, fare.fare ?: "DESCONOCIDO")
                    dialog.dismiss()
                    fareDialog = null
                }
                container.addView(rowView)
            }
        }

        val dismissAction = {
            viewModel.cancelarSeleccionTarifa()
            dialog.dismiss()
            fareDialog = null
        }

        btnCancelTop.setOnClickListener { dismissAction() }
        btnCancelBottom.setOnClickListener { dismissAction() }

        fareDialog = dialog
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(gpsReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(request, networkCallback)

        // Activación del lector NFC en modo ininterrumpido.
        val options = Bundle()
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            options
        )
        actualizarEstadosMenuLateral()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(gpsReceiver)
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkCallback)
        nfcAdapter?.disableReaderMode(this)
    }

    /** Callback invocado al detectar una tarjeta física en el campo NFC. */
    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        val uidBytes = tag.id
        val uidHex = Utils.bytesToHex(uidBytes)

        runOnUiThread {
            if (viewModel.uiState.value is ValidadorState.Listo) {
                val categoria = cobroBotones.categoriaPendiente
                val esMinimo = cobroBotones.esMinimoPendiente
                
                // Transferencia del control al ViewModel para el procesamiento del cobro.
                viewModel.procesarCobroNfc(tag, uidBytes, uidHex, categoria, esMinimo)
            }
        }
    }
}
