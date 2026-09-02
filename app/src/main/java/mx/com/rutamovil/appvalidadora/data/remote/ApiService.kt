package mx.com.rutamovil.appvalidadora.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mx.com.rutamovil.appvalidadora.common.Constants
import mx.com.rutamovil.appvalidadora.data.remote.models.CardsResponse
import mx.com.rutamovil.appvalidadora.data.remote.models.Fare
import mx.com.rutamovil.appvalidadora.data.remote.models.LoginResponse
import mx.com.rutamovil.appvalidadora.data.remote.models.TarifasResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Singleton encargado de centralizar las llamadas a la API de servicios del validador.
 * Proporciona métodos síncronos y asíncronos para la sincronización de transacciones, 
 * autenticación y obtención de parámetros operativos.
 */
object ApiService {
    private const val TAG = "ApiService"
    
    /** Tipo de medio JSON para los cuerpos de las peticiones. */
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /** Cliente OkHttp configurado con tiempos de espera optimizados para movilidad. */
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Instancia de Gson configurada con un formato de fecha específico para interoperabilidad. */
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    /** Token de autenticación del validador activo. */
    private var validatorToken: String? = null
    
    /** Prefijo para el encabezado de autorización. */
    private const val tokenType = "Bearer"

    /**
     * Establece el token de autenticación a nivel global para las peticiones posteriores.
     *
     * @param token Cadena con el token JWT.
     */
    fun setValidatorToken(token: String?) {
        validatorToken = token
    }

    /**
     * Sincroniza un conjunto de transacciones locales con el servidor de forma síncrona.
     *
     * @param transactions Arreglo JSON conteniendo los datos de las transacciones.
     * @return Objeto JSON con la respuesta del servidor o nulo si falla la comunicación.
     */
    fun syncTransactionsSync(transactions: JsonArray): JsonObject? {
        val token = validatorToken ?: return null
        val body = JsonObject().apply {
            add("transactions", transactions)
        }

        Log.d(TAG, "🔄 SYNC REQUEST: $body")

        val request = Request.Builder()
            .url("${Constants.API_BASE_URL}validator/sync")
            .post(body.toString().toRequestBody(JSON))
            .addHeader("Authorization", "$tokenType $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "🔄 SYNC RESPONSE: Code ${response.code}, Body: $responseBody")
                
                if (response.isSuccessful) {
                    gson.fromJson(responseBody, JsonObject::class.java)
                } else {
                    Log.e(TAG, "Sync failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}")
            null
        }
    }

    /**
     * Interfaz genérica para el manejo de respuestas asíncronas de la API.
     *
     * @param T Tipo de dato esperado en la respuesta exitosa.
     */
    interface ApiCallback<T> {
        /** Se invoca al recibir una respuesta exitosa del servidor. */
        fun onSuccess(response: T)
        /** Se invoca ante errores de red, servidor o procesamiento. */
        fun onError(error: String)
    }

    /**
     * Realiza una petición asíncrona de inicio de sesión.
     *
     * @param email Correo electrónico.
     * @param pass Contraseña.
     * @param callback Interfaz de retorno para los resultados.
     */
    fun login(email: String, pass: String, callback: ApiCallback<LoginResponse>) {
        val jsonBody = JsonObject().apply {
            addProperty("email", email)
            addProperty("password", pass)
        }

        val request = Request.Builder()
            .url("${Constants.API_BASE_URL}auth/login")
            .post(jsonBody.toString().toRequestBody(JSON))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Connection error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string() ?: ""
                    Log.d("TARIFAS", "Código respuesta: ${it.code}")
                    Log.d("TARIFAS", "Respuesta: $body")

                    if (it.isSuccessful) {
                        try {
                            val loginResponse = gson.fromJson(body, LoginResponse::class.java)
                            if (loginResponse.status) {
                                validatorToken = loginResponse.getToken()
                                callback.onSuccess(loginResponse)
                            } else {
                                callback.onError("Invalid credentials")
                            }
                        } catch (e: Exception) {
                            callback.onError("Parsing error")
                        }
                    } else {
                        callback.onError("HTTP ${it.code}: $body")
                    }
                }
            }
        })
    }

    /**
     * Obtiene el listado de tarifas vigentes para el validador autenticado.
     *
     * @param callback Interfaz de retorno con el listado de tarifas [Fare].
     */
    fun obtenerTarifas(callback: ApiCallback<List<Fare>>) {
        val token = validatorToken ?: return callback.onError("No session token")

        Log.d("TARIFAS", "URL: ${Constants.API_BASE_URL}validator/fares")
        Log.d("TARIFAS", "Token: ${token.take(40)}...")

        val request = Request.Builder()
            .url("${Constants.API_BASE_URL}validator/fares")
            .get()
            .addHeader("Authorization", "$tokenType $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Connection error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string() ?: ""
                    Log.d("TARIFAS", "Código respuesta: ${it.code}")
                    Log.d("TARIFAS", "Respuesta: $body")

                    if (it.isSuccessful) {
                        try {
                            val responseObj = gson.fromJson(body, TarifasResponse::class.java)
                            if (responseObj.status) {
                                callback.onSuccess(responseObj.data)
                            } else {
                                callback.onError("Server error")
                            }
                        } catch (e: Exception) {
                            callback.onError("Parsing error")
                        }
                    } else {
                        callback.onError("HTTP ${it.code}")
                    }
                }
            }
        })
    }

    /**
     * Notifica al servidor la ejecución de un cobro sobre una tarjeta específica.
     *
     * @param cardUuid Identificador único de la tarjeta.
     * @param monto Cantidad monetaria cobrada.
     * @param callback Interfaz de retorno para confirmar el éxito del cargo.
     */
    fun realizarCobro(cardUuid: String, monto: Double, callback: ApiCallback<CardsResponse>) {
        if (validatorToken == null || validatorToken!!.isEmpty()) {
            Log.e("API_COBRO", "❌ Error: No hay token de validator configurado")
            callback.onError("No autenticado")
            return
        }

        val url = "${Constants.API_BASE_URL}validator/charge"
        val formattedAmount = "%.2f".format(Locale.US, monto)
        val jsonRequest = "{\"card_uuid\":\"$cardUuid\",\"amount\":\"$formattedAmount\"}"

        Log.d("API_COBRO", "🚀 INICIANDO PETICIÓN DE COBRO")
        Log.d("API_COBRO", "📍 URL: $url")
        Log.d("API_COBRO", "📦 BODY: $jsonRequest")
        Log.d("API_COBRO", "🔑 TOKEN: ${validatorToken!!.take(15)}...")

        val body = jsonRequest.toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "$tokenType $validatorToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API_ERROR", "❌ FALLO DE RED: ${e.message}")
                callback.onError("Error de conexión: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string() ?: ""
                    val code = it.code
                    Log.d("API_COBRO", "📡 RESPUESTA RECIBIDA - Código HTTP: $code")

                    if (it.isSuccessful) {
                        try {
                            val cardResponse = gson.fromJson(responseBody, CardsResponse::class.java)
                            if (cardResponse.status) {
                                val nuevoSaldo = cardResponse.data?.balance ?: "N/A"
                                Log.d("API_COBRO", "✅ COBRO EXITOSO EN SERVIDOR")
                                Log.d("API_COBRO", "💰 NUEVO SALDO REPORTADO: $$nuevoSaldo")
                                callback.onSuccess(cardResponse)
                            } else {
                                Log.w("API_COBRO", "⚠️ SERVIDOR RESPONDIÓ ERROR (status=false)")
                                Log.w("API_COBRO", "📄 CUERPO: $responseBody")
                                callback.onError("Error en la respuesta del servidor")
                            }
                        } catch (e: Exception) {
                            Log.e("API_COBRO", "❌ ERROR PARSEANDO JSON: ${e.message}")
                            Log.e("API_COBRO", "📄 CUERPO QUE FALLÓ: $responseBody")
                            callback.onError("Error al procesar respuesta: ${e.message}")
                        }
                    } else {
                        Log.e("API_COBRO", "❌ ERROR HTTP $code")
                        Log.e("API_COBRO", "📄 CUERPO: $responseBody")
                        callback.onError("Error HTTP $code: $responseBody")
                    }
                }
            }
        })
    }
}
