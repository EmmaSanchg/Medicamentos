package com.example.medicamentos

import android.os.*
import android.speech.tts.TextToSpeech
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private lateinit var vibrator: Vibrator
    private var detectedText = ""
    private var isVibrating = false
    private var isTtsReady = false // Control para saber si el motor de voz cargó

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración del vibrador según la versión de Android
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Inicializar TTS
        tts = TextToSpeech(this, this)

        setContent {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(
                    onTap = { leerDatos() },
                    onDoubleTap = { tts?.stop() }
                )
            }) {
                CameraPreview { text -> gestionarVibracion(text) }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
            tts?.setSpeechRate(0.9f) // Velocidad global base
            isTtsReady = true
            saludarUsuario()
        }
    }

    // Se ejecuta cada vez que el usuario entra o regresa a la app
    override fun onResume() {
        super.onResume()
        if (isTtsReady) {
            saludarUsuario()
        }
    }

    private fun saludarUsuario() {
        tts?.speak("Medicamentos lista", TextToSpeech.QUEUE_FLUSH, null, "saludo_id")
    }

    private fun gestionarVibracion(text: String) {
        if (text.isNotBlank()) {
            detectedText = text

            if (!isVibrating) {
                isVibrating = true
                val pattern = longArrayOf(0, 1000, 1000) // 1s vibra, 1s pausa
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }
            }
        } else {
            detenerVibracion()
        }
    }

    private fun detenerVibracion() {
        isVibrating = false
        vibrator.cancel()
    }

    private fun leerDatos() {
        if (detectedText.isBlank()) {
            tts?.speak("No detecto texto claro.", TextToSpeech.QUEUE_FLUSH, null, "error_id")
            return
        }

        val textoLimpio = detectedText.replace("\n", " ").trim()

        // Ajuste final de velocidad a 0.9f para mejor claridad
        tts?.setSpeechRate(0.9f)
        tts?.setPitch(1.0f)

        tts?.speak(textoLimpio, TextToSpeech.QUEUE_FLUSH, null, "lectura_total")
    }

    // --- MANEJO DEL CICLO DE VIDA PARA AHORRO DE BATERÍA ---

    override fun onPause() {
        super.onPause()
        pararTodo()
    }

    override fun onStop() {
        super.onStop()
        pararTodo()
    }

    private fun pararTodo() {
        detenerVibracion()
        tts?.stop()
        detectedText = "" // Limpiar buffer para evitar lecturas fantasmas al volver
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        vibrator.cancel()
    }
}

@Composable
fun CameraPreview(onTextDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            analizarImagen(imageProxy, onTextDetected)
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analizarImagen(imageProxy: ImageProxy, onTextDetected: (String) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
            .addOnSuccessListener { onTextDetected(it.text) }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}