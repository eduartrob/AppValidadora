package mx.com.rutamovil.appvalidadora.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.com.rutamovil.appvalidadora.common.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Cliente de red encargado de gestionar las peticiones HTTP hacia el servidor remoto.
 * Implementa lógica de autenticación, refresco de tokens y comunicación segura, 
 * incluyendo soporte para certificados SSL no verificados en entornos de desarrollo/pruebas.
 *
 * @property context Contexto de la aplicación para acceder a las preferencias compartidas.
 */
class APIClient(context: Context) {

    /** Preferencias compartidas para el almacenamiento persistente del token JWT y marcas de tiempo. */
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /** Instancia configurada de OkHttpClient. */
    private val client: OkHttpClient = getUnsafeOkHttpClient()

    companion object {
        private const val TAG = "APIClient"

        /** Tipo de contenido para las peticiones con cuerpo JSON. */
        private val JSON =
            "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Realiza el proceso de inicio de sesión de un usuario validador.
     * En caso de éxito, el token JWT recibido es almacenado localmente para su uso posterior.
     *
     * @param email Correo electrónico del usuario.
     * @param pass Contraseña en texto plano.
     * @return Verdadero si el inicio de sesión fue exitoso y el token fue almacenado, falso en caso contrario.
     */
    suspend fun login(email: String, pass: String): Boolean =
        withContext(Dispatchers.IO) {

            try {
                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("password", pass)
                }

                val url = "${Constants.API_BASE_URL}auth/login"

                Log.d(TAG, "LOGIN REQUEST")
                Log.d(TAG, "URL: $url")
                Log.d(TAG, "Email: $email")

                val request = Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(
                        jsonBody
                            .toString()
                            .toRequestBody(JSON)
                    )
                    .build()

                client.newCall(request).execute().use { response ->

                    val responseBody =
                        response.body?.string() ?: ""

                    Log.d(TAG, "LOGIN RESPONSE")
                    Log.d(TAG, "HTTP Code: ${response.code}")
                    Log.d(TAG, "Response: $responseBody")


                    if (!response.isSuccessful) {
                        Log.e(
                            TAG,
                            "Login falló. HTTP ${response.code}"
                        )

                        return@withContext false
                    }


                    val jsonResponse = JSONObject(responseBody)


                    val status =
                        jsonResponse.optBoolean("status", false)

                    if (!status) {

                        val message =
                            jsonResponse.optString(
                                "message",
                                "Login rechazado por el servidor"
                            )

                        Log.e(
                            TAG,
                            "Login rechazado. Status=false. Mensaje: $message"
                        )

                        return@withContext false
                    }

                    if (!jsonResponse.has("data")) {
                        Log.e(
                            TAG,
                            "La respuesta no contiene el objeto 'data'"
                        )

                        return@withContext false
                    }

                    val data =
                        jsonResponse.getJSONObject("data")

                    var roleName = ""

                    if (data.has("user")) {
                        val userObj = data.getJSONObject("user")
                        
                        if (userObj.has("role")) {
                            roleName = userObj.getString("role")
                        } else if (userObj.has("roles")) {
                            roleName = userObj.optJSONArray("roles")?.toString() ?: ""
                        }
                    } 
                    
                    if (roleName.isEmpty() && data.has("role")) {
                        roleName = data.getString("role")
                    } else if (roleName.isEmpty() && data.has("role_id")) {
                        roleName = data.getString("role_id")
                    }

                    // Validación estricta del rol autorizado para la aplicación Validadora.
                    val normalizedRole = roleName.lowercase()
                    val isAuthorized = normalizedRole.contains("validator") || normalizedRole.contains("validador")

                    if (!isAuthorized) {
                        return@withContext false
                    }

                    if (!data.has("access_token")) {

                        Log.e(
                            TAG,
                            "La respuesta no contiene access_token"
                        )

                        return@withContext false
                    }

                    val token =
                        data.getString("access_token")

                    if (token.isBlank()) {

                        Log.e(
                            TAG,
                            "El access_token está vacío"
                        )

                        return@withContext false
                    }

                    prefs.edit()
                        .putString("jwt_token", token)
                        .putLong("login_timestamp", System.currentTimeMillis())
                        .apply()

                    Log.d(
                        TAG,
                        "Login exitoso. Token guardado."
                    )

                    return@withContext true
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Exception en login",
                    e
                )

                return@withContext false
            }
        }

    /**
     * Solicita al servidor el refresco del token de acceso actual utilizando el token existente.
     * Actualiza el almacenamiento local con el nuevo token recibido.
     *
     * @return Verdadero si el token fue refrescado y almacenado correctamente, falso en caso contrario.
     */
    suspend fun refreshToken(): Boolean =
        withContext(Dispatchers.IO) {

            try {

                val currentToken =
                    getToken()

                if (currentToken.isEmpty()) {

                    Log.e(
                        TAG,
                        "No hay token para refrescar"
                    )

                    return@withContext false
                }

                val url =
                    "${Constants.API_BASE_URL}auth/refresh"

                Log.d(TAG, "REFRESH TOKEN REQUEST")
                Log.d(TAG, "URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .header(
                        "Authorization",
                        "Bearer $currentToken"
                    )
                    .header(
                        "Content-Type",
                        "application/json"
                    )
                    .header(
                        "Accept",
                        "application/json"
                    )
                    .post(
                        "".toRequestBody(JSON)
                    )
                    .build()

                client.newCall(request).execute().use { response ->

                    val responseBody =
                        response.body?.string() ?: ""

                    Log.d(
                        TAG,
                        "Refresh HTTP: ${response.code}"
                    )

                    Log.d(
                        TAG,
                        "Refresh Response: $responseBody"
                    )

                    if (!response.isSuccessful) {
                        return@withContext false
                    }

                    val jsonResponse =
                        JSONObject(responseBody)

                    val status =
                        jsonResponse.optBoolean(
                            "status",
                            false
                        )

                    if (!status) {

                        Log.e(
                            TAG,
                            "Refresh rechazado"
                        )

                        return@withContext false
                    }

                    val data =
                        jsonResponse.getJSONObject("data")

                    val newAccessToken =
                        data.getString("access_token")

                    if (newAccessToken.isBlank()) {
                        return@withContext false
                    }

                    prefs.edit()
                        .putString(
                            "jwt_token",
                            newAccessToken
                        )
                        .apply()

                    Log.d(
                        TAG,
                        "Token refrescado correctamente"
                    )

                    return@withContext true
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Exception en refreshToken",
                    e
                )

                return@withContext false
            }
        }


    /**
     * Envía la información de una tarjeta recién inicializada al servidor para su registro oficial.
     * Maneja automáticamente la expiración del token intentando un refresco antes de fallar.
     *
     * @param uid Identificador físico único de la tarjeta.
     * @param masterKey Llave maestra asignada (no enviada directamente, se usa por contexto).
     * @param systemKey Llave de sistema asignada (no enviada directamente, se usa por contexto).
     * @param discountType Tipo de descuento o perfil de tarifa de la tarjeta.
     * @return Un par que contiene un Booleano (éxito/error) y una Cadena (mensaje informativo).
     */
    suspend fun initializeCard(
        uid: String,
        masterKey: String,
        systemKey: String,
        discountType: String
    ): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {

            try {

                var token = getToken()

                if (token.isEmpty()) {

                    Log.e(
                        TAG,
                        "No hay sesión activa"
                    )

                    return@withContext Pair(
                        false,
                        "No hay sesión activa"
                    )
                }

                val url =
                    "${Constants.API_BASE_URL}superadmin/prepaid_card/create"

                val jsonBody = JSONObject().apply {
                    put("uuid", uid)
                    put("discount_type", discountType)
                }

                Log.d(TAG, "INITIALIZE CARD REQUEST")
                Log.d(TAG, "URL: $url")
                Log.d(TAG, "Body: ${jsonBody.toString(2)}")
                Log.d(TAG, "UUID: $uid")
                Log.d(TAG, "Discount Type: $discountType")

                var request = Request.Builder()
                    .url(url)
                    .header(
                        "Authorization",
                        "Bearer $token"
                    )
                    .header(
                        "Content-Type",
                        "application/json"
                    )
                    .header(
                        "Accept",
                        "application/json"
                    )
                    .post(
                        jsonBody
                            .toString()
                            .toRequestBody(JSON)
                    )
                    .build()

                var response =
                    client.newCall(request).execute()

                var responseBody =
                    response.body?.string() ?: ""

                Log.d(TAG, "INITIALIZE CARD RESPONSE")
                Log.d(TAG, "HTTP Code: ${response.code}")
                Log.d(TAG, "Response: $responseBody")


                if (response.code == 401) {

                    response.close()

                    Log.d(
                        TAG,
                        "Token expirado. Intentando refrescar..."
                    )

                    val refreshed =
                        refreshToken()

                    if (!refreshed) {

                        Log.e(
                            TAG,
                            "No se pudo refrescar el token"
                        )

                        return@withContext Pair(
                            false,
                            "Sesión expirada, inicie sesión nuevamente"
                        )
                    }

                    // Obtener nuevo token
                    token = getToken()

                    Log.d(
                        TAG,
                        "Token refrescado. Reintentando petición..."
                    )

                    request = Request.Builder()
                        .url(url)
                        .header(
                            "Authorization",
                            "Bearer $token"
                        )
                        .header(
                            "Content-Type",
                            "application/json"
                        )
                        .header(
                            "Accept",
                            "application/json"
                        )
                        .post(
                            jsonBody
                                .toString()
                                .toRequestBody(JSON)
                        )
                        .build()

                    response =
                        client.newCall(request).execute()

                    responseBody =
                        response.body?.string() ?: ""

                    Log.d(
                        TAG,
                        "Reintento HTTP: ${response.code}"
                    )

                    Log.d(
                        TAG,
                        "Reintento Response: $responseBody"
                    )
                }

                // ============================================================
                // CORRECCIÓN: Manejo del código 422 (Tarjeta ya registrada)
                // ============================================================
                if (response.code == 422) {
                    Log.d(TAG, "La tarjeta ya existía en la API (422). Continuando con el flujo como exitoso.")
                    return@withContext Pair(
                        true,
                        "Sincronización exitosa (La tarjeta ya estaba registrada)"
                    )
                }

                if (response.isSuccessful) {

                    val jsonResponse =
                        JSONObject(responseBody)

                    val status =
                        jsonResponse.optBoolean(
                            "status",
                            false
                        )

                    if (status) {

                        Log.d(
                            TAG,
                            "Tarjeta creada correctamente"
                        )

                        return@withContext Pair(
                            true,
                            "Sincronización exitosa"
                        )

                    } else {

                        val message =
                            jsonResponse.optString(
                                "message",
                                "El servidor rechazó la tarjeta"
                            )

                        Log.e(
                            TAG,
                            "API rechazó la tarjeta: $message"
                        )

                        return@withContext Pair(
                            false,
                            message
                        )
                    }
                }

                val errorMessage = try {

                    val errorJson =
                        JSONObject(responseBody)

                    errorJson.optString(
                        "message",
                        "Error HTTP: ${response.code}"
                    )

                } catch (e: Exception) {

                    "Error HTTP: ${response.code}"
                }

                Log.e(
                    TAG,
                    "Error initializeCard: $errorMessage"
                )

                return@withContext Pair(
                    false,
                    errorMessage
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Exception en initializeCard",
                    e
                )

                return@withContext Pair(
                    false,
                    e.message ?: "Error desconocido"
                )
            }
        }


    /**
     * Recupera el token JWT almacenado en las preferencias locales.
     *
     * @return Cadena con el token o una cadena vacía si no existe.
     */
    private fun getToken(): String {

        return prefs.getString(
            "jwt_token",
            ""
        ) ?: ""
    }

    /**
     * Almacena manualmente un token JWT en las preferencias compartidas.
     *
     * @param token Cadena con el token de acceso a persistir.
     */
    fun saveToken(token: String) {

        prefs.edit()
            .putString("jwt_token", token)
            .apply()

        Log.d(
            TAG,
            "Token guardado: ${
                token.substring(
                    0,
                    minOf(20, token.length)
                )
            }..."
        )
    }

    /**
     * Configura y devuelve una instancia de OkHttpClient que ignora la validación de certificados SSL.
     * ADVERTENCIA: Este método solo debe utilizarse en entornos controlados de desarrollo, 
     * ya que compromete la seguridad de la comunicación.
     *
     * @return Instancia de OkHttpClient con configuración SSL permisiva.
     */
    private fun getUnsafeOkHttpClient(): OkHttpClient {

        try {

            val trustAllCerts =
                arrayOf<TrustManager>(
                    object : X509TrustManager {

                        override fun checkClientTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) {
                        }

                        override fun checkServerTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) {
                        }

                        override fun getAcceptedIssuers():
                                Array<X509Certificate> =
                            arrayOf()
                    }
                )

            val sslContext =
                SSLContext.getInstance("SSL")

            sslContext.init(
                null,
                trustAllCerts,
                SecureRandom()
            )

            return OkHttpClient.Builder()
                .connectTimeout(
                    30,
                    TimeUnit.SECONDS
                )
                .writeTimeout(
                    30,
                    TimeUnit.SECONDS
                )
                .readTimeout(
                    30,
                    TimeUnit.SECONDS
                )
                .sslSocketFactory(
                    sslContext.socketFactory,
                    trustAllCerts[0] as X509TrustManager
                )
                .hostnameVerifier { _, _ ->
                    true
                }
                .build()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "No se pudo crear cliente SSL inseguro",
                e
            )

            return OkHttpClient.Builder()
                .connectTimeout(
                    30,
                    TimeUnit.SECONDS
                )
                .writeTimeout(
                    30,
                    TimeUnit.SECONDS
                )
                .readTimeout(
                    30,
                    TimeUnit.SECONDS
                )
                .build()
        }
    }
}
