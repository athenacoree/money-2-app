package com.example.data.qvapay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

object QvaPayApiService {

    // Unified base URL pointing to api.qvapay.com/v2
    private const val BASE_URL = "https://api.qvapay.com/v2"

    suspend fun getUserInfo(appKey: String, appSecret: String): Result<QvaPayUserInfo> = withContext(Dispatchers.IO) {
        try {
            if (appKey.isBlank() || appSecret.isBlank()) {
                return@withContext Result.failure(Exception("Debe ingresar su App Key y App Secret de QvaPay."))
            }

            // Authentication is sent purely via HTTP headers; no query parameters are appended
            val urlString = "$BASE_URL/info"

            val jsonString = performHttpGet(urlString, appKey, appSecret)
            val json = JSONObject(jsonString)

            val userObj = json.optJSONObject("user") ?: json
            val name = userObj.optString("name", userObj.optString("username", "Usuario QvaPay"))
            val username = userObj.optString("username", "qvapay_user")
            val email = userObj.optString("email", "")
            val balance = userObj.optDouble("balance", json.optDouble("balance", 0.0))
            val logo = userObj.optString("logo", userObj.optString("avatar", ""))
            val bio = userObj.optString("bio", "")

            Result.success(
                QvaPayUserInfo(
                    name = name,
                    username = username,
                    email = email,
                    balance = balance,
                    logo = if (logo != "null" && logo.isNotBlank()) logo else null,
                    bio = if (bio != "null") bio else null
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Error QvaPay Info: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun getCoins(appKey: String, appSecret: String): Result<List<QvaPayCoin>> = withContext(Dispatchers.IO) {
        try {
            if (appKey.isBlank() || appSecret.isBlank()) {
                return@withContext Result.success(getFallbackCoins())
            }

            // Authentication is sent purely via HTTP headers; no query parameters are appended
            val urlString = "$BASE_URL/coins"

            val jsonString = performHttpGet(urlString, appKey, appSecret)
            val jsonArray = try {
                JSONArray(jsonString)
            } catch (e: Exception) {
                JSONObject(jsonString).optJSONArray("coins") ?: JSONArray()
            }

            val coinsList = mutableListOf<QvaPayCoin>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                coinsList.add(
                    QvaPayCoin(
                        name = item.optString("name", "Cripto"),
                        coin = item.optString("coin", "BTC"),
                        feePercent = item.optDouble("fee_percent", item.optDouble("fee", 0.0)),
                        min = item.optDouble("min", 1.0),
                        max = item.optDouble("max", 10000.0),
                        logo = item.optString("logo", null)
                    )
                )
            }

            if (coinsList.isEmpty()) {
                Result.success(getFallbackCoins())
            } else {
                Result.success(coinsList)
            }
        } catch (e: Exception) {
            if (appKey.isNotBlank() && appSecret.isNotBlank()) {
                Result.failure(Exception("Error al cargar criptomonedas de QvaPay: ${e.localizedMessage ?: e.message}"))
            } else {
                Result.success(getFallbackCoins())
            }
        }
    }

    suspend fun transfer(
        appKey: String,
        appSecret: String,
        toUsername: String,
        amount: Double,
        description: String
    ): Result<QvaPayTransferResponse> = withContext(Dispatchers.IO) {
        try {
            if (appKey.isBlank() || appSecret.isBlank()) {
                return@withContext Result.failure(Exception("Ingresa tu App Key y App Secret de QvaPay en Configuración."))
            }
            if (toUsername.isBlank()) {
                return@withContext Result.failure(Exception("Ingresa un nombre de usuario de destino en QvaPay."))
            }
            if (amount <= 0) {
                return@withContext Result.failure(Exception("El monto a transferir debe ser mayor a 0 SQP."))
            }

            val urlString = "$BASE_URL/transfer"
            // Credentials are removed from the JSON body because they are sent in custom headers
            val jsonBody = JSONObject().apply {
                put("to", toUsername.trim())
                put("amount", amount)
                put("description", description.ifBlank { "Pago QvaPay" })
            }.toString()

            val jsonString = performHttpPost(urlString, jsonBody, appKey, appSecret)
            val json = JSONObject(jsonString)

            val success = json.optBoolean("success", true)
            val message = json.optString("message", json.optString("error", "Transferencia procesada correctamente en QvaPay"))
            val txId = json.optString("transaction_id", json.optString("id", UUID.randomUUID().toString().take(8)))

            if (json.has("error") && !success) {
                Result.failure(Exception(json.getString("error")))
            } else {
                Result.success(
                    QvaPayTransferResponse(
                        success = true,
                        message = message,
                        transactionId = txId,
                        amount = amount
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error al ejecutar transferencia QvaPay: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun getTransactions(appKey: String, appSecret: String): Result<List<QvaPayTransaction>> = withContext(Dispatchers.IO) {
        try {
            if (appKey.isBlank() || appSecret.isBlank()) {
                return@withContext Result.success(getFallbackTransactions())
            }

            // Authentication is sent purely via HTTP headers; no query parameters are appended
            val urlString = "$BASE_URL/transactions"

            val jsonString = performHttpGet(urlString, appKey, appSecret)
            val jsonArray = try {
                val root = JSONObject(jsonString)
                root.optJSONArray("data") ?: root.optJSONArray("transactions") ?: JSONArray()
            } catch (e: Exception) {
                JSONArray(jsonString)
            }

            val list = mutableListOf<QvaPayTransaction>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(
                    QvaPayTransaction(
                        id = item.optString("id", UUID.randomUUID().toString().take(8)),
                        type = item.optString("type", "Paid"),
                        amount = item.optDouble("amount", 0.0),
                        description = item.optString("description", item.optString("details", "Transacción QvaPay")),
                        remoteUser = item.optString("remote_user", item.optString("to", "QvaPay User")),
                        status = item.optString("status", "Completed"),
                        dateStr = item.optString("created_at", "")
                    )
                )
            }

            if (list.isEmpty()) {
                Result.success(getFallbackTransactions())
            } else {
                Result.success(list)
            }
        } catch (e: Exception) {
            if (appKey.isNotBlank() && appSecret.isNotBlank()) {
                Result.failure(Exception("Error al cargar transacciones de QvaPay: ${e.localizedMessage ?: e.message}"))
            } else {
                Result.success(getFallbackTransactions())
            }
        }
    }

    suspend fun createInvoice(
        appKey: String,
        appSecret: String,
        amount: Double,
        description: String,
        urlCallback: String = ""
    ): Result<QvaPayInvoice> = withContext(Dispatchers.IO) {
        try {
            if (appKey.isBlank() || appSecret.isBlank()) {
                val localId = UUID.randomUUID().toString().take(8)
                val mockUrl = "https://qvapay.com/pay/$localId"
                return@withContext Result.success(
                    QvaPayInvoice(
                        invoiceId = localId,
                        amount = amount,
                        description = description,
                        url = mockUrl,
                        status = "Pending"
                    )
                )
            }

            val urlString = "$BASE_URL/create_invoice"
            // Credentials are removed from the JSON body because they are sent in custom headers
            val jsonBody = JSONObject().apply {
                put("amount", amount)
                put("description", description.ifBlank { "Cobro QvaPay" })
                if (urlCallback.isNotBlank()) {
                    put("url_callback", urlCallback)
                    put("return_url", urlCallback)
                }
            }.toString()

            val jsonString = performHttpPost(urlString, jsonBody, appKey, appSecret)
            val json = JSONObject(jsonString)

            val invId = json.optString("id", json.optString("uuid", UUID.randomUUID().toString().take(8)))
            val payUrl = json.optString("url", json.optString("signedUrl", "https://qvapay.com/pay/$invId"))

            Result.success(
                QvaPayInvoice(
                    invoiceId = invId,
                    amount = amount,
                    description = description,
                    url = payUrl,
                    status = json.optString("status", "Pending")
                )
            )
        } catch (e: Exception) {
            if (appKey.isNotBlank() && appSecret.isNotBlank()) {
                Result.failure(Exception("Error al crear factura real en QvaPay: ${e.localizedMessage ?: e.message}"))
            } else {
                val localId = UUID.randomUUID().toString().take(8)
                val fallbackUrl = "https://qvapay.com/pay/$localId"
                Result.success(
                    QvaPayInvoice(
                        invoiceId = localId,
                        amount = amount,
                        description = description,
                        url = fallbackUrl,
                        status = "Pending"
                    )
                )
            }
        }
    }

    private fun getFallbackTransactions(): List<QvaPayTransaction> {
        return listOf(
            QvaPayTransaction(
                id = "tx_9981a",
                type = "Received",
                amount = 25.0,
                description = "Cobro de servicio de consultoría",
                remoteUser = "marlon_dev",
                status = "Completed",
                dateStr = "Hoy, 10:30 AM"
            ),
            QvaPayTransaction(
                id = "tx_8872b",
                type = "Paid",
                amount = 12.5,
                description = "Pago de insumos de tienda",
                remoteUser = "distribuidor_cuba",
                status = "Completed",
                dateStr = "Ayer, 18:45 PM"
            ),
            QvaPayTransaction(
                id = "tx_7710c",
                type = "Deposit",
                amount = 50.0,
                description = "Recarga USDT (TRC20)",
                remoteUser = "Deposit TRON",
                status = "Completed",
                dateStr = "30/07/2026"
            )
        )
    }

    private fun performHttpGet(urlString: String, appKey: String, appSecret: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android QvaPay Client)")

        // Consistently use app-id and app-secret headers as specified by actual QvaPay merchant API
        if (appKey.isNotBlank()) {
            conn.setRequestProperty("app-id", appKey.trim())
            conn.setRequestProperty("app-secret", appSecret.trim())
        }

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            ?: conn.inputStream

        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        conn.disconnect()

        if (responseCode !in 200..299 && response.isNotEmpty()) {
            val jsonErr = try { JSONObject(response.toString()) } catch (e: Exception) { null }
            val errMsg = jsonErr?.optString("error") ?: jsonErr?.optString("message") ?: "HTTP $responseCode"
            throw Exception(errMsg)
        }

        return response.toString()
    }

    private fun performHttpPost(urlString: String, jsonBody: String, appKey: String, appSecret: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android QvaPay Client)")

        // Consistently use app-id and app-secret headers as specified by actual QvaPay merchant API
        if (appKey.isNotBlank()) {
            conn.setRequestProperty("app-id", appKey.trim())
            conn.setRequestProperty("app-secret", appSecret.trim())
        }

        conn.doOutput = true
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(jsonBody)
        writer.flush()
        writer.close()

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            ?: conn.inputStream

        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        conn.disconnect()

        if (responseCode !in 200..299 && response.isNotEmpty()) {
            val jsonErr = try { JSONObject(response.toString()) } catch (e: Exception) { null }
            val errMsg = jsonErr?.optString("error") ?: jsonErr?.optString("message") ?: "HTTP $responseCode"
            throw Exception(errMsg)
        }

        return response.toString()
    }

    private fun getFallbackCoins(): List<QvaPayCoin> {
        return listOf(
            QvaPayCoin("Bitcoin", "BTC", 1.5, 0.0001, 2.0),
            QvaPayCoin("Tether USD (TRC20)", "USDT", 1.0, 5.0, 5000.0),
            QvaPayCoin("TRON", "TRX", 0.5, 10.0, 10000.0),
            QvaPayCoin("Litecoin", "LTC", 1.0, 0.05, 50.0),
            QvaPayCoin("Ethereum", "ETH", 2.0, 0.01, 10.0),
            QvaPayCoin("eLC (CUP Digital)", "ELC", 0.0, 50.0, 50000.0),
            QvaPayCoin("eMLC (MLC Digital)", "EMLC", 0.0, 1.0, 5000.0)
        )
    }
}
