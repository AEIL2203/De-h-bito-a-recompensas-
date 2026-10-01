package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

sealed class NsfwCheckResult {
    data object Safe : NsfwCheckResult()
    data class Unsafe(val confidence: Float) : NsfwCheckResult()
    data class Error(val message: String) : NsfwCheckResult()
}

object NsfwDetector {
    private const val TAG = "NsfwDetector"
    private const val MODEL_FILE_NAME = "nsfw_model.tflite"
    private const val LABELS_FILE_NAME = "labels.txt"
    private const val UNSAFE_THRESHOLD = 0.55f

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputWidth = 224
    private var inputHeight = 224
    private var inputDataType = DataType.FLOAT32
    private var outputDataType = DataType.FLOAT32
    private var initialized = false

    @Synchronized
    private fun initialize(context: Context): Boolean {
        if (initialized) return interpreter != null
        initialized = true
        return runCatching {
            val descriptor = context.assets.openFd(MODEL_FILE_NAME)
            val model = descriptor.createInputStream().channel.map(
                java.nio.channels.FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
            val instance = Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })
            val input = instance.getInputTensor(0)
            val shape = input.shape()
            inputHeight = shape.getOrElse(1) { 224 }
            inputWidth = shape.getOrElse(2) { 224 }
            inputDataType = input.dataType()
            outputDataType = instance.getOutputTensor(0).dataType()
            labels = context.assets.open(LABELS_FILE_NAME).use { stream ->
                BufferedReader(InputStreamReader(stream)).readLines().filter(String::isNotBlank)
            }
            interpreter = instance
            true
        }.onFailure { Log.e(TAG, "No se pudo cargar el detector de imágenes", it) }.getOrDefault(false)
    }

    suspend fun analyzeBitmap(context: Context, bitmap: Bitmap): NsfwCheckResult =
        withContext(Dispatchers.Default) {
            if (!initialize(context.applicationContext)) {
                return@withContext NsfwCheckResult.Error("No se pudo iniciar el análisis de seguridad")
            }
            runCatching {
                val instance = interpreter ?: error("Detector no disponible")
                val inputBuffer = bitmapToBuffer(bitmap)
                val outputTensor = instance.getOutputTensor(0)
                val outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes()).order(ByteOrder.nativeOrder())
                instance.run(inputBuffer, outputBuffer)
                outputBuffer.rewind()
                val scores = when (outputDataType) {
                    DataType.UINT8 -> FloatArray(outputTensor.numBytes()) { outputBuffer.get().toInt().and(0xFF) / 255f }
                    else -> FloatArray(outputTensor.numBytes() / 4) { outputBuffer.float }
                }.let(::normalizeScores)
                val unsafeIndex = labels.indexOfFirst {
                    it.contains("unsafe", true) || it.contains("nsfw", true) ||
                        it.contains("nude", true) || it.contains("porn", true)
                }.takeIf { it >= 0 } ?: if (scores.size > 1) 1 else 0
                val unsafeScore = scores.getOrElse(unsafeIndex) { 0f }
                Log.d(TAG, "Resultado de moderación: ${scores.joinToString { "%.3f".format(it) }}")
                if (unsafeScore >= UNSAFE_THRESHOLD) NsfwCheckResult.Unsafe(unsafeScore)
                else NsfwCheckResult.Safe
            }.getOrElse {
                Log.e(TAG, "Error analizando imagen", it)
                NsfwCheckResult.Error("No se pudo analizar la imagen")
            }
        }

    private fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val safeBitmap = ensureSoftwareBitmap(bitmap)
        val resized = Bitmap.createScaledBitmap(safeBitmap, inputWidth, inputHeight, true)
        if (safeBitmap !== bitmap) safeBitmap.recycle()

        val bytesPerChannel = if (inputDataType == DataType.FLOAT32) 4 else 1
        val buffer = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * bytesPerChannel)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        resized.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        pixels.forEach { pixel ->
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            if (inputDataType == DataType.FLOAT32) {
                buffer.putFloat(red.toFloat())
                buffer.putFloat(green.toFloat())
                buffer.putFloat(blue.toFloat())
            } else {
                buffer.put(red.toByte())
                buffer.put(green.toByte())
                buffer.put(blue.toByte())
            }
        }
        resized.recycle()
        buffer.rewind()
        return buffer
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.config != Bitmap.Config.HARDWARE) return bitmap
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
            ?: throw IllegalStateException("No se pudo convertir el bitmap hardware a software")
    }

    private fun normalizeScores(values: FloatArray): FloatArray {
        if (values.size <= 1 || values.all { it in 0f..1f } && values.sum() in 0.95f..1.05f) return values
        val max = values.maxOrNull() ?: return values
        val exponents = values.map { exp((it - max).toDouble()) }
        val total = exponents.sum().takeIf { it > 0 } ?: return values
        return exponents.map { (it / total).toFloat() }.toFloatArray()
    }
}
