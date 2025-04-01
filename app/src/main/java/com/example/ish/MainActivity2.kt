package com.example.ish

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.Menu
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
 
class MainActivity2 : AppCompatActivity(), TextToSpeech.OnInitListener {

        private lateinit var cameraExecutor: ExecutorService
        private lateinit var imageCapture: ImageCapture
        private lateinit var webSocket: WebSocket
        private lateinit var previewView: PreviewView
        private lateinit var tts: TextToSpeech
        private val SERVER_URI = "ws://192.168.173.177:5000"
        private lateinit var textView: TextView
        private lateinit var ttsButton: ImageView
        private lateinit var bck: ImageView
        private var isUsingFrontCamera: Boolean = false
        private var cnt: Int = 0
        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main2)

                previewView = findViewById(R.id.previewView)
                var captureButton: ImageView = findViewById(R.id.captureButton)
                textView = findViewById(R.id.textView)
                ttsButton = findViewById(R.id.speak)
                bck = findViewById(R.id.home)
                tts = TextToSpeech(this, this)

                startCamera()
                startWebSocket()
                bck.setOnClickListener {
                        intent = Intent(this, menu::class.java)
                        startActivity(intent)
                }
                findViewById<ImageView>(R.id.swap).setOnClickListener{
                        swap()
                }

                cameraExecutor = Executors.newSingleThreadExecutor()

                val scheduler = Executors.newScheduledThreadPool(1)
                var scheduledTask: ScheduledFuture<*>? = null


                captureButton.setOnTouchListener { v, event ->
                        when (event.action) {
                                android.view.MotionEvent.ACTION_DOWN -> {
                                        captureButton.isEnabled = false
                                        Handler().postDelayed({ captureButton.isEnabled = true }, 50)
                                        runOnUiThread {
                                                takePhoto()
                                        }
                                        true
                                }
                                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                        captureButton.isEnabled = true
                                        true
                                }
                                else -> false
                        }
                }


                ttsButton.setOnClickListener {
                        speakOutText()
                }
        }

        private fun startCamera() {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

                cameraProviderFuture.addListener(Runnable {
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder()
                                .setTargetResolution(Size(224, 224))
                                .build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                        imageCapture = ImageCapture.Builder()
                                .setTargetResolution(Size(224, 224))
                                .build()

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                        } catch (exc: Exception) {
                                Log.e("CameraX", "Camera binding failed", exc)
                        }
                }, ContextCompat.getMainExecutor(this))
        }

        private fun swap() {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
                cameraProviderFuture.addListener(Runnable {
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                        isUsingFrontCamera = !isUsingFrontCamera
                        val cameraSelector = if (isUsingFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        // Set the Preview with target resolution of 224x224
                        val preview = Preview.Builder()
                                .setTargetResolution(Size(224, 224))
                                .build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                        // Set ImageCapture with the same resolution of 224x224
                        imageCapture = ImageCapture.Builder()
                                .setTargetResolution(Size(224, 224))
                                .build()

                        try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                        } catch (exc: Exception) {
                                Log.e("CameraX", "Camera binding failed", exc)
                        }
                }, ContextCompat.getMainExecutor(this))
        }

        private fun startWebSocket() {
                val client = OkHttpClient()
                val request = Request.Builder().url(SERVER_URI).build()
                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                                Log.d("WebSocket", "WebSocket Connection Opened")
                        }

                        override fun onMessage(webSocket: WebSocket, txt: String) {
                                Log.d("WebSocket", "Message Received: $txt")
                                runOnUiThread {
                                        textView.text = textView.text.toString() + txt
                                }
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                                Log.e("WebSocket", "WebSocket connection failed: ${t.message}")
                                t.printStackTrace()
                        }
                })
                client.dispatcher.executorService.shutdown()
        }

        private fun takePhoto() {
                val outputOptions = ImageCapture.OutputFileOptions.Builder(createFile()).build()
                imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                Log.d("ImageCapture", "Photo capture succeeded: ${outputFileResults.savedUri}")
                                val uri = outputFileResults.savedUri
                                if (uri != null) {
                                        val inputStream = contentResolver.openInputStream(uri)
                                        val bitmap = BitmapFactory.decodeStream(inputStream)
                                        if (bitmap != null) {
                                                sendFrameToWebSocket(bitmap)
                                                Log.d("ImageCapture", "Sending image to WebSocket")
                                        } else {
                                                Log.e("ImageCapture", "Failed to decode bitmap from URI")
                                        }
                                } else {
                                        Log.e("ImageCapture", "Saved URI is null")
                                }
                        }

                        override fun onError(exception: ImageCaptureException) {
                                Log.e("ImageCapture", "Photo capture failed: ${exception.message}", exception)
                        }
                })
        }

        private fun createFile(): File {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        }

        private fun sendFrameToWebSocket(bitmap: Bitmap) {
                val frameData = bitmapToBase64(bitmap)
                if (frameData.isNotEmpty()) {
                        Log.d("WebSocket", "Sending frame data to WebSocket")
                        webSocket.send(frameData)
                } else {
                        Log.e("WebSocket", "Failed to convert bitmap to Base64")
                }
        }

        private fun bitmapToBase64(bitmap: Bitmap): String {
                val targetWidth = 800
                val targetHeight = 800

                val resizedBitmap = resizeBitmap(bitmap, targetWidth, targetHeight)

                val byteArrayOutputStream = ByteArrayOutputStream()
                return try {
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                        val byteArray = byteArrayOutputStream.toByteArray()
                        Base64.encodeToString(byteArray, Base64.DEFAULT).also {
                                Log.i("BitmapToBase64", "Image successfully converted to Base64")
                        }
                } catch (e: Exception) {
                        Log.e("BitmapToBase64", "Error converting bitmap to Base64: ${e.message}", e)
                        ""
                } finally {
                        try {
                                byteArrayOutputStream.close()
                        } catch (e: IOException) {
                                Log.e("BitmapToBase64", "Error closing ByteArrayOutputStream: ${e.message}", e)
                        }
                }
        }

        private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
                val width = bitmap.width
                val height = bitmap.height

                if (width <= maxWidth && height <= maxHeight) {
                        return bitmap
                }

                val aspectRatio = width.toFloat() / height.toFloat()
                val targetWidth: Int
                val targetHeight: Int

                if (aspectRatio > 1) {
                        targetWidth = maxWidth
                        targetHeight = (maxWidth / aspectRatio).toInt()
                } else {
                        targetHeight = maxHeight
                        targetWidth = (maxHeight * aspectRatio).toInt()
                }

                return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }

        override fun onInit(status: Int) {
                if (status == TextToSpeech.SUCCESS) {
                        val result = tts.setLanguage(Locale.US)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("TTS", "The language is not supported!")
                        }
                } else {
                        Log.e("TTS", "Initialization failed!")
                }
        }

        private fun speakOutText() {
                val text = textView.text.toString()
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }

        override fun onDestroy() {
                super.onDestroy()
                cameraExecutor.shutdown()
                webSocket.close(1000, "App Destroyed")
                Log.d("Camera", "Camera Destroyed")

                if (::tts.isInitialized) {
                        tts.stop()
                        tts.shutdown()
                }
        }
}
