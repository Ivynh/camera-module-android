package com.example.cameraxmlvalidator

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.MappedByteBuffer

// Data class to hold results from both models
data class ValidationResult(
    val isBlurry: Boolean,
    val isDocument: Boolean,
    val blurConfidence: Float,
    val documentConfidence: Float,
    val blurLabel: String,
    val documentLabel: String
)

class ImageValidator(private val context: Context) {

    companion object {
        const val TAG = "ImageValidator"
        // --- IMPORTANT ---
        // Make sure your compatible model files in the assets folder have these exact names
        const val BLUR_MODEL = "final_blur_model_compatible.tflite"
        const val DOC_MODEL = "final_document_model_compatible.tflite"
    }

    private var blurInterpreter: Interpreter? = null
    private var docInterpreter: Interpreter? = null

    // Input dimensions will be read from the model
    private var modelInputWidth = 0
    private var modelInputHeight = 0

    init {
        try {
            val options = Interpreter.Options()
            options.setUseXNNPACK(true) // Use optimized CPU path

            // Load the Blur Model
            blurInterpreter = Interpreter(loadModelFile(BLUR_MODEL), options)
            Log.d(TAG, "SUCCESS: Blur model loaded.")

            // Load the Document Model
            docInterpreter = Interpreter(loadModelFile(DOC_MODEL), options)
            Log.d(TAG, "SUCCESS: Document model loaded.")

            // Read input dimensions (assuming both models have the same input size)
            val inputTensor = blurInterpreter!!.getInputTensor(0)
            val inputShape = inputTensor.shape()
            modelInputHeight = inputShape[1]
            modelInputWidth = inputShape[2]
            Log.d(TAG, "Model Input Shape: [${inputShape.joinToString()}]")

        } catch (e: Throwable) {
            val errorMessage = "FATAL ERROR: Could not load models from assets. Check filenames and Logcat."
            Log.e(TAG, errorMessage, e)
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        return FileUtil.loadMappedFile(context, modelName)
    }

    fun analyzeImage(bitmap: Bitmap): ValidationResult {
        if (blurInterpreter == null || docInterpreter == null) {
            Log.e(TAG, "One or more interpreters not initialized.")
            return ValidationResult(false, false, 0f, 0f, "Error", "Error")
        }

        // --- Blur Model Analysis ---
        val blurOutput = runInference(blurInterpreter!!, bitmap)
        val blurScore = blurOutput.floatArray[0]
        // *** CORRECTED LOGIC ***
        // A LOW score means it's blurry.
        val isBlurry = blurScore < 0.7
        val blurLabel = if (isBlurry) "blurry" else "sharp"
        val blurConfidence = if (isBlurry) 1 - blurScore else blurScore
        Log.d(TAG, "Blur Analysis: Label=${blurLabel}, Confidence=${blurConfidence}, Raw Score=${blurScore}")

        // --- Document Model Analysis ---
        val docOutput = runInference(docInterpreter!!, bitmap)
        val docScore = docOutput.floatArray[0]
        // *** CORRECTED LOGIC ***
        // A LOW score means it's a document.
        val isDocument = docScore < 0.5
        val docLabel = if (isDocument) "document" else "other"
        val docConfidence = if (isDocument) 1 - docScore else docScore
        Log.d(TAG, "Doc Analysis: Label=${docLabel}, Confidence=${docConfidence}, Raw Score=${docScore}")

        return ValidationResult(
            isBlurry = isBlurry,
            isDocument = isDocument,
            blurConfidence = blurConfidence,
            documentConfidence = docConfidence,
            blurLabel = blurLabel,
            documentLabel = docLabel
        )
    }

    private fun runInference(interpreter: Interpreter, bitmap: Bitmap): TensorBuffer {
        val inputDataType = interpreter.getInputTensor(0).dataType()
        var tensorImage = TensorImage(inputDataType)
        tensorImage.load(bitmap)

        // The ImageProcessor now uses the dynamic width and height read from the model
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(modelInputHeight, modelInputWidth, ResizeOp.ResizeMethod.BILINEAR))
            // The Rescaling layer is now part of the model itself, so no normalization is needed here.
            .build()
        tensorImage = imageProcessor.process(tensorImage)

        val outputTensor = interpreter.getOutputTensor(0)
        val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())

        interpreter.run(tensorImage.buffer, outputBuffer.buffer.rewind())

        return outputBuffer
    }
}
