package org.landmarkscollector.elm

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mlkit.common.MlKitException
import org.landmarkscollector.elm.State.Steady
import org.landmarkscollector.elm.State.Steady.ReadyToStartRecording
import org.landmarkscollector.hands.HandLandmarkerHelper
import org.landmarkscollector.hands.LandmarkerListener
import org.landmarkscollector.hands.OverlayView
import org.landmarkscollector.hands.ResultBundle
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.detectors.DetectorProcessor
import org.landmarkscollector.mlkit.detectors.DetectorResult
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class
)
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ElmCameraScreen(
    state: State,
    onDirectoryChosen: (directory: Uri) -> Unit,
    onGestureNameChanged: (gestureName: String) -> Unit,
    onStartRecordingPressed: () -> Unit,
    onHandResults: (results: HandLandmarkerResult) -> Unit,
    onFacePoseResults: (imageProxy: ImageProxy, result: DetectorResult) -> Unit
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
                            onHandResults(resultBundle.result)
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
                                            onFacePoseResults(imageProxy, detectorResult)
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
                    factory = { graphicOverlay },
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { overlayView },
                    modifier = Modifier.fillMaxSize()
                )
                Column {
                    when (state) {
                        is Steady -> {
                            var gestureName by remember { mutableStateOf("") }
                            val pickPathLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.OpenDocumentTree(),
                                onResult = { uri ->
                                    if (uri != null) {
                                        onDirectoryChosen(uri)
                                    }
                                }
                            )
                            Button(onClick = {
                                pickPathLauncher.launch(null)
                            }) {
                                val action = if (state is ReadyToStartRecording)
                                    "Change"
                                else
                                    "Choose"
                                Text("$action directory to save CSVs", fontSize = 25.sp)
                            }
                            val focusManager = LocalFocusManager.current
                            OutlinedTextField(
                                value = gestureName,
                                maxLines = 1,
                                onValueChange = { text ->
                                    gestureName = text
                                    onGestureNameChanged(text)
                                },
                                label = { Text("Gesture Name (.csv file name)") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                )
                            )
                            if (state is ReadyToStartRecording) {
                                Button(
                                    onClick = onStartRecordingPressed
                                ) {
                                    Text("Start Recording", fontSize = 25.sp)
                                }
                                Text(
                                    "Gesture-directory path: ${state.directoryUri.path}",
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Gesture will be recorded in files: ${state.gestureName}_N.csv",
                                    fontSize = 16.sp
                                )
                            }
                        }

                        is State.Recording.PreparingForTheNextRecording -> {
                            val gestureNum = state.gestureNum.dec()
                            if (gestureNum != UInt.MIN_VALUE) {
                                Text(
                                    "Gesture ${state.gestureName} #: $gestureNum is saved",
                                    fontSize = 20.sp
                                )
                            }
                            Text(
                                "⏱️Recording #${state.gestureNum} will start in: ${state.delayTicks}",
                                fontSize = 25.sp
                            )
                        }

                        is State.Recording.RecordingMotion -> {
                            Text(
                                "Gesture ${state.gestureName} #: ${state.gestureNum}",
                                fontSize = 20.sp
                            )
                            Text("\uD83D\uDD34 Live: Recording", fontSize = 23.sp)
                            Text("Time seconds left: ${state.timeLeft}", fontSize = 25.sp)
                        }

                        is State.Recording.SavingPreviousMotion -> {
                            Text("💾 Saving...", fontSize = 25.sp)
                            Text("Done: ${state.savingProgress}%", fontSize = 25.sp)
                            Text(
                                "Gesture ${state.gestureName} #: ${state.gestureNum}",
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
