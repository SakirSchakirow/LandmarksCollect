package org.landmarkscollector

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mlkit.common.MlKitException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.landmarkscollector.hands.HandLandmarkerHelper
import org.landmarkscollector.hands.LandmarkerListener
import org.landmarkscollector.hands.OverlayView
import org.landmarkscollector.hands.ResultBundle
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.detectors.DetectorProcessor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

const val DELAY_SECS = 3
const val GESTURES_NUM = 10
const val MAX_FRAMES_SECS = 5

@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class
)
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen(
    viewModel: CameraScreenViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

    val previewView: PreviewView = remember {
        PreviewView(context)
            .apply { scaleType = PreviewView.ScaleType.FILL_START }
    }

    val overlayView: OverlayView = remember {
        OverlayView(context)
    }
    val graphicOverlay: GraphicOverlay = remember {
        GraphicOverlay(context)
    }

    val cameraSelector: MutableState<CameraSelector> = remember {
        mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA)
    }

    if (cameraPermissionState.hasPermission) {

        LaunchedEffect(previewView) {
            lateinit var handLandmarkerHelper: HandLandmarkerHelper
            lateinit var detectorProcessor: DetectorProcessor
            val backgroundExecutor = Executors.newSingleThreadExecutor()
            backgroundExecutor.execute {
                handLandmarkerHelper = HandLandmarkerHelper(context, object : LandmarkerListener {

                    override fun onError(error: String) {
                        Log.e("CameraScreen", error)
                    }

                    override fun onResults(resultBundle: ResultBundle) {
                        ContextCompat.getMainExecutor(context).execute {
                            viewModel.onHandResults(resultBundle.result)
                            with(overlayView) {
                                setResults(
                                    handLandmarkerResults = resultBundle.result,
                                    imageHeight = resultBundle.inputImageHeight,
                                    imageWidth = resultBundle.inputImageWidth,
                                    runningMode = RunningMode.LIVE_STREAM
                                )
                                invalidate()
                            }
                        }
                    }
                })
            }

            detectorProcessor = DetectorProcessor(context)

            with(suspendCoroutine { continuation ->
                ProcessCameraProvider.getInstance(context).also { future ->
                    future.addListener(
                        {
                            continuation.resume(future.get())
                        },
                        ContextCompat.getMainExecutor(context)
                    )
                }
            }) {
                var needUpdateGraphicOverlayImageSourceInfo = true
                unbindAll()
                bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector.value,
                    Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(previewView.display.rotation)
                        .build()
                        .apply { setSurfaceProvider(previewView.surfaceProvider) },
                    ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(previewView.display.rotation)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                        // The analyzer can then be assigned to the instance
                        .apply {
                            setAnalyzer(backgroundExecutor) { imageProxy ->
                                handLandmarkerHelper.detectLiveStream(
                                    imageProxy = imageProxy,
                                    isFrontCamera = cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA
                                )
                                if (needUpdateGraphicOverlayImageSourceInfo) {
                                    val isImageFlipped =
                                        true//TODOlensFacing == CameraSelector.LENS_FACING_FRONT
                                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                                        graphicOverlay.setImageSourceInfo(
                                            imageProxy.width,
                                            imageProxy.height,
                                            isImageFlipped
                                        )
                                    } else {
                                        graphicOverlay.setImageSourceInfo(
                                            imageProxy.height,
                                            imageProxy.width,
                                            isImageFlipped
                                        )
                                    }
                                    needUpdateGraphicOverlayImageSourceInfo = false
                                }
                                try {
                                    detectorProcessor
                                        .processImageProxy(
                                            imageProxy,
                                            graphicOverlay
                                        ) { detectorResult ->
                                            viewModel.onFacePoseResults(imageProxy, detectorResult)
                                        }
                                } catch (e: MlKitException) {
                                    Log.e(
                                        "CameraScreen",
                                        "Failed to process image. Error: " + e.localizedMessage
                                    )
                                }
                            }
                        }
                )
            }
        }
    }

    PermissionRequired(
        permissionState = cameraPermissionState,
        permissionNotGrantedContent = {
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Request permission")
            }
        },
        permissionNotAvailableContent = { /* ... */ }
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { overlayView },
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { graphicOverlay },
                    modifier = Modifier.fillMaxSize()
                )
                Column {


                    val timerScope = rememberCoroutineScope()

                    var timerDelayTicks by remember { mutableStateOf(DELAY_SECS) }
                    var timerTicks by remember { mutableStateOf(0) }
                    val gesturesNum by viewModel.gesturesNum.collectAsState()
                    val isRecordingButtonDisabled by viewModel.isRecordingButtonDisabled
                        .collectAsState(
                            initial = false
                        )

                    if (timerDelayTicks > 0) {
                        Text("⏱️Recording will start in: $timerDelayTicks", fontSize = 25.sp)
                    } else {
                        Text("\uD83D\uDD34 Live: Recording", fontSize = 25.sp)
                    }
                    Text("Time seconds left: $timerTicks", fontSize = 25.sp)
                    Text("Gesture #: $gesturesNum", fontSize = 25.sp)
                    Button(
                        enabled = isRecordingButtonDisabled.not(),
                        onClick = {
                            timerScope.launch {
                                viewModel.setIsRecordingOn(true)
                                while (gesturesNum < GESTURES_NUM) {
                                    timerDelayTicks = DELAY_SECS
                                    timerTicks = MAX_FRAMES_SECS
                                    while (timerDelayTicks > 0) {
                                        delay(1.seconds)
                                        timerDelayTicks--
                                    }
                                    while (timerTicks > 0) {
                                        delay(1.seconds)
                                        timerTicks--
                                    }
                                    viewModel.setGesturesNum(context, gesturesNum + 1)
                                }
                                viewModel.setIsRecordingOn(false)
                            }
                        }) {
                        Text("Start Recording", fontSize = 25.sp)
                    }

                    val pickPathLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree(),
                        onResult = { uri ->
                            uri?.let(viewModel::setCurrentDirectory)
                        }
                    )
                    Button(onClick = {
                        pickPathLauncher.launch(null)
                    }) {
                        Text("Choose directory to save CSVs", fontSize = 25.sp)
                    }

                    val gestureName by viewModel.currentGestureName.collectAsState()
                    val focusManager = LocalFocusManager.current
                    OutlinedTextField(
                        value = gestureName ?: "",
                        maxLines = 1,
                        onValueChange = { text ->
                            viewModel.setGestureName(text)
                        },
                        label = { Text("Gesture Name (.csv file name)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                }
            }
        }
    }
}
