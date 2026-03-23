package com.example.medicamentos

import android.media.AudioManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: AudioManager
    private var detectedText = ""
    private var isVibrating = false
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        tts = TextToSpeech(this, this)

        setContent {
            MainScreenWrapper()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun MainScreenWrapper() {
        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

        if (cameraPermissionState.status.isGranted) {
            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { leerDatos() },
                        onDoubleTap = { tts?.stop() }
                    )
                }) {
                CameraPreview { text -> gestionarVibracion(text) }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Se necesita la cámara para identificar medicamentos.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Permitir cámara")
                }
            }

            // Solo lanzamos la petición visual aquí, la voz la maneja onResume
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
            tts?.setSpeechRate(0.85f)
            isTtsReady = true
            // Intentar hablar inmediatamente después de inicializar
            gestionarMensajesDeInicio()
        }
    }

    override fun onResume() {
        super.onResume()
        ajustarVolumenAlto()
        if (isTtsReady) {
            gestionarMensajesDeInicio()
        }
    }

    override fun onPause() {
        // CORTE TOTAL DE AUDIO AL SALIR
        tts?.stop()
        detenerVibracion()
        super.onPause()
    }

    private fun gestionarMensajesDeInicio() {
        val tienePermiso = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            tts?.speak("Medicamentos lista", TextToSpeech.QUEUE_FLUSH, null, "saludo_id")
        } else {
            val aviso = "Esta aplicación necesita permiso para usar la cámara. Por favor, pide a alguien de confianza que acepte los permisos en pantalla para comenzar."
            // Usamos un pequeño delay manual para que el audio no se corte
            Handler(Looper.getMainLooper()).postDelayed({
                tts?.speak(aviso, TextToSpeech.QUEUE_FLUSH, null, "permiso_aviso_id")
            }, 500)
        }
    }

    private fun ajustarVolumenAlto() {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val vol90 = (maxVol * 0.9).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol90, 0)
    }

    private fun gestionarVibracion(text: String) {
        if (text.isNotBlank()) {
            detectedText = text
            if (!isVibrating) {
                isVibrating = true
                val pattern = longArrayOf(0, 800, 800)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION") vibrator.vibrate(pattern, 0)
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
        tts?.speak(textoLimpio, TextToSpeech.QUEUE_FLUSH, null, "lectura_total")
    }

    override fun onDestroy() {
        tts?.shutdown()
        vibrator.cancel()
        super.onDestroy()
    }
}

// ... (CameraPreview y analizarImagen se mantienen igual) ...

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
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
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