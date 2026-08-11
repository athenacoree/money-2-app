package com.example.data.receiver

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

data class SmsModelResult(
    val tipo: String,       // "ingreso" | "gasto" | "sin_clasificar"
    val monto: Double,
    val moneda: String,     // "CUP" | "MLC" | "USD"
    val confianza: String   // "alta" | "media" | "baja"
)

class SmsModelParser(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val vocab = mutableMapOf<String, Int>()
    private var isInitialized = false

    init {
        try {
            // 1. Load vocabulary from assets
            val vocabString = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(vocabString)
            jsonObject.keys().forEach { key ->
                vocab[key] = jsonObject.getInt(key)
            }

            // 2. Load TFLite model from assets
            val modelBuffer = loadModelFile(context, "sms_model.tflite")
            interpreter = Interpreter(modelBuffer)
            isInitialized = true
            Log.d("SmsModelParser", "SmsModelParser successfully initialized.")
        } catch (e: Throwable) {
            Log.e("SmsModelParser", "Failed to initialize SmsModelParser", e)
        }
    }

    private fun loadModelFile(context: Context, filename: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun tokenize(text: String): List<String> {
        return text.split(Regex("[\\s:,;\\(\\)\\[\\]\\!\\?\\-\\+]+")).filter { it.isNotEmpty() }
    }

    private fun cleanAndParseFloat(token: String): Double? {
        return try {
            // Keep only digits and dots/periods
            val clean = token.replace(Regex("[^\\d\\.]"), "")
            clean.toDoubleOrNull()
        } catch (e: Throwable) {
            null
        }
    }

    fun parseMessage(body: String): SmsModelResult {
        if (!isInitialized || interpreter == null) {
            Log.e("SmsModelParser", "Parser not initialized, returning sin_clasificar.")
            return SmsModelResult("sin_clasificar", 0.0, "CUP", "baja")
        }

        try {
            val tokens = tokenize(body)
            val maxLen = 64
            val inputIds = IntArray(maxLen) { 0 }

            for (i in 0 until minOf(tokens.size, maxLen)) {
                val token = tokens[i]
                inputIds[i] = vocab[token] ?: vocab["<UNK>"] ?: 1
            }

            // TensorFlow Lite expects inputs shaped as [batch_size, sequence_length]
            val inputIdsBatch = Array(1) { inputIds }

            // Outputs map conforming to signature list mapping
            val outputs = mutableMapOf<Int, Any>()

            val outConfianza = Array(1) { FloatArray(3) }
            val outMontoMask = Array(1) { Array(64) { FloatArray(1) } }
            val outMoneda = Array(1) { FloatArray(3) }
            val outTipo = Array(1) { FloatArray(3) }

            // Index mapping matching serving_default signature runner details
            outputs[0] = outConfianza
            outputs[1] = outMontoMask
            outputs[2] = outMoneda
            outputs[3] = outTipo

            // Run model inference
            interpreter?.runForMultipleInputsOutputs(arrayOf(inputIdsBatch), outputs)

            // Extract tipo classification
            val tipoProbs = outTipo[0]
            val tipoIdx = tipoProbs.indices.maxByOrNull { tipoProbs[it] } ?: 2
            val tipo = when (tipoIdx) {
                0 -> "ingreso"
                1 -> "gasto"
                else -> "sin_clasificar"
            }

            // Extract moneda classification
            val monedaProbs = outMoneda[0]
            val monedaIdx = monedaProbs.indices.maxByOrNull { monedaProbs[it] } ?: 0
            val moneda = when (monedaIdx) {
                0 -> "CUP"
                1 -> "MLC"
                2 -> "USD"
                else -> "CUP"
            }

            // Extract confianza classification
            val confianzaProbs = outConfianza[0]
            val confianzaIdx = confianzaProbs.indices.maxByOrNull { confianzaProbs[it] } ?: 2
            val confianza = when (confianzaIdx) {
                0 -> "alta"
                1 -> "media"
                else -> "baja"
            }

            // Extract amount using predicted sequence mask
            val mask = outMontoMask[0]
            var maxProb = 0f
            var bestTokenIndex = -1

            for (j in 0 until minOf(tokens.size, maxLen)) {
                val prob = mask[j][0]
                if (prob > maxProb) {
                    maxProb = prob
                    bestTokenIndex = j
                }
            }

            var monto = 0.0
            if (bestTokenIndex != -1 && maxProb >= 0.3f) {
                val tokenCandidate = tokens[bestTokenIndex]
                val parsed = cleanAndParseFloat(tokenCandidate)
                if (parsed != null) {
                    // VALIDACIÓN ANTI-ALUCINACIÓN: verify token parsed double physically exists inside SMS body
                    val cleanBody = body.lowercase()
                    // Extract numeric substring from token
                    val cleanTokenDigits = tokenCandidate.replace(Regex("[^\\d\\.]"), "")
                    if (cleanTokenDigits.isNotEmpty() && cleanBody.contains(cleanTokenDigits)) {
                        monto = parsed
                    } else {
                        Log.w("SmsModelParser", "Anti-hallucination rejected amount $parsed because $cleanTokenDigits was not found in original SMS.")
                    }
                }
            }

            Log.d("SmsModelParser", "Parsed SMS -> tipo: $tipo, monto: $monto, moneda: $moneda, confianza: $confianza (maxProb: $maxProb)")
            return SmsModelResult(tipo, monto, moneda, confianza)

        } catch (e: Throwable) {
            Log.e("SmsModelParser", "Exception running TFLite model on SMS, fallback.", e)
            return SmsModelResult("sin_clasificar", 0.0, "CUP", "baja")
        }
    }

    fun close() {
        interpreter?.close()
    }
}
