package com.example.cameraxmlvalidator

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxmlvalidator.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var imageValidator: ImageValidator

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Activity Result Launcher for picking an image from the gallery
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it)
                // Once we have the bitmap, we can show it and validate it
                showImageAndValidate(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image from gallery", e)
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        imageValidator = ImageValidator(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up listeners for the buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        viewBinding.uploadButton.setOnClickListener { openGallery() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File.createTempFile(
            "capture_",
            ".jpg",
            externalCacheDir
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    showFeedback("Photo capture failed.", true)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(TAG, msg)
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    showImageAndValidate(bitmap)
                }
            }
        )
    }

    // New function to handle showing the preview and running validation
    private fun showImageAndValidate(bitmap: Bitmap) {
        // Show the captured image and hide the camera preview
        viewBinding.capturedImageView.setImageBitmap(bitmap)
        viewBinding.capturedImageView.visibility = View.VISIBLE
        viewBinding.viewFinder.visibility = View.GONE

        // Change UI to "Retake" mode
        viewBinding.imageCaptureButton.text = getString(R.string.retake)
        viewBinding.uploadButton.visibility = View.GONE // Hide upload button in preview mode

        // Run validation
        validateImage(bitmap)
    }

    private fun validateImage(bitmap: Bitmap) {
        val result = imageValidator.analyzeImage(bitmap)

        // The success condition now checks both models
        val isSuccess = !result.isBlurry && result.isDocument

        if (isSuccess) {
            showFeedback("Success! Image is clear and is a document.", false)
            // *** UPDATED LOGIC FOR SUCCESS CASE ***
            // Keep the button enabled and set its function to retake the photo.
            viewBinding.imageCaptureButton.text = getString(R.string.retake)
            viewBinding.imageCaptureButton.isEnabled = true
            viewBinding.imageCaptureButton.setOnClickListener { resetToCameraView() }
        } else {
            // Build a specific error message
            val errorMessages = mutableListOf<String>()
            if (result.isBlurry) {
                errorMessages.add("Image is blurry")
            }
            if (!result.isDocument) {
                errorMessages.add("Not a document")
            }
            val feedbackMessage = errorMessages.joinToString(" & ") + ". Please try again."

            showFeedback(feedbackMessage, true)
            viewBinding.imageCaptureButton.setOnClickListener { resetToCameraView() }
        }
    }

    private fun resetToCameraView() {
        viewBinding.capturedImageView.visibility = View.GONE
        viewBinding.viewFinder.visibility = View.VISIBLE
        viewBinding.validationText.visibility = View.GONE

        // Restore buttons to their initial state
        viewBinding.imageCaptureButton.text = getString(R.string.take_photo)
        viewBinding.imageCaptureButton.isEnabled = true
        viewBinding.uploadButton.visibility = View.VISIBLE

        // Reset the click listener to take a photo
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
    }

    private fun showFeedback(message: String, isError: Boolean) {
        viewBinding.validationText.text = message
        viewBinding.validationText.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (isError) android.R.color.holo_red_dark else android.R.color.holo_green_dark
            )
        )
        viewBinding.validationText.visibility = View.VISIBLE
        viewBinding.validationText.bringToFront()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).toTypedArray()
    }
}
