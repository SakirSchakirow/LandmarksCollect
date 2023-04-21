package org.landmarkscollector.motionRecording

import android.Manifest
import android.net.Uri
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.common.MlKitException
import org.landmarkscollector.data.Landmark.Hand
import org.landmarkscollector.hands.HandLandmarkerHelper
import org.landmarkscollector.hands.OverlayView
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.detectors.DetectorProcessor
import org.landmarkscollector.mlkit.detectors.DetectorResult
import org.landmarkscollector.motionRecording.CamerasInfo.CamerasAvailable.AllTypes
import org.landmarkscollector.motionRecording.CamerasInfo.CamerasAvailable.OnlyBack
import org.landmarkscollector.motionRecording.CamerasInfo.CamerasAvailable.OnlyFacing
import org.landmarkscollector.motionRecording.State.LiveCamera.Steady
import org.landmarkscollector.motionRecording.State.LiveCamera.Steady.ReadyToStartRecording
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
internal fun CameraScreen(
    state: State,
    onCameraInfoReceived: (camerasInfo: CamerasInfo) -> Unit,
    onCameraToggle: (isChecked: Boolean) -> Unit,
    onDirectoryChosen: (directory: Uri) -> Unit,
    onGestureNameChanged: (gestureName: String) -> Unit,
    onStartRecordingPressed: () -> Unit,
    onPauseRecordingPressed: () -> Unit,
    onResumeRecordingPressed: () -> Unit,
    onStopRecordingPressed: () -> Unit,
    onHandResults: (handsResults: List<List<Hand>>) -> Unit,
    onFacePoseResults: (result: DetectorResult) -> Unit,
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

    if (cameraPermissionState.hasPermission) {

        LaunchedEffect(
            key1 = previewView,
            key2 = if (state is State.LiveCamera) state.camera.isCurrentFrontFacing else state
        ) {
            lateinit var handLandmarkerHelper: HandLandmarkerHelper
            val backgroundExecutor = Executors.newSingleThreadExecutor()
            backgroundExecutor.execute {
                handLandmarkerHelper = HandLandmarkerHelper(
                    context = context,
                    landmarkerListener = HandsLandmarkerListener(
                        mainExecutor = ContextCompat.getMainExecutor(context),
                        overlayView = overlayView,
                        onHandResults = onHandResults
                    )
                )
            }

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
                if (state is State.LiveCamera) {
                    val detectorProcessor = DetectorProcessor(context)
                    var needUpdateGraphicOverlayImageSourceInfo = true
                    unbindAll()
                    bindToLifecycle(
                        lifecycleOwner,
                        when (state.camera) {
                            is AllTypes -> if (state.camera.isCurrentFrontFacing) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                            OnlyBack -> CameraSelector.DEFAULT_BACK_CAMERA
                            OnlyFacing -> CameraSelector.DEFAULT_FRONT_CAMERA
                        },
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
                                        isFrontCamera = state.camera.isCurrentFrontFacing
                                    )
                                    if (needUpdateGraphicOverlayImageSourceInfo) {
                                        val isImageFlipped = state.camera.isCurrentFrontFacing
                                        val rotationDegrees =
                                            imageProxy.imageInfo.rotationDegrees
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
                                                onFacePoseResults(detectorResult)
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
                } else {
                    val isCameraAvailable = availableCameraInfos.isNotEmpty()
                    val isFrontCameraAvailable = availableCameraInfos
                        .find { it.lensFacing == CameraSelector.LENS_FACING_FRONT } != null
                    val isBackCameraAvailable = availableCameraInfos
                        .find { it.lensFacing == CameraSelector.LENS_FACING_BACK } != null
                    val camerasInfo = when {
                        isCameraAvailable.not() -> CamerasInfo.NoCameras
                        isFrontCameraAvailable && isBackCameraAvailable -> AllTypes()
                        isFrontCameraAvailable -> OnlyFacing
                        isBackCameraAvailable -> OnlyBack
                        else -> CamerasInfo.UnknownCameras
                    }
                    onCameraInfoReceived(camerasInfo)
                }
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
        StateContent(
            state,
            onCameraToggle,
            onDirectoryChosen,
            onGestureNameChanged,
            onStartRecordingPressed,
            onPauseRecordingPressed,
            onResumeRecordingPressed,
            onStopRecordingPressed,
            previewView,
            graphicOverlay,
            overlayView
        )
    }
}

@Composable
internal fun StateContent(
    state: State,
    onCameraToggle: (isChecked: Boolean) -> Unit,
    onDirectoryChosen: (directory: Uri) -> Unit,
    onGestureNameChanged: (gestureName: String) -> Unit,
    onStartRecordingPressed: () -> Unit,
    onPauseRecordingPressed: () -> Unit,
    onResumeRecordingPressed: () -> Unit,
    onStopRecordingPressed: () -> Unit,
    previewView: PreviewView,
    graphicOverlay: GraphicOverlay,
    overlayView: OverlayView,
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
                    is Steady -> Steady(
                        state,
                        onCameraToggle,
                        onDirectoryChosen,
                        onGestureNameChanged,
                        onStartRecordingPressed
                    )

                    is State.LiveCamera.Recording.Pausable -> Pausable(
                        state, onPauseRecordingPressed,
                        onResumeRecordingPressed,
                        onStopRecordingPressed
                    )

                    is State.LiveCamera.Recording.SavingPreviousMotion -> SavingPreviousMotion(state)
                    State.NoCameraPermitted -> {
                        /* Nothing to build - maybe specific design is required */
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Steady(
    state: Steady,
    onCameraToggle: (isChecked: Boolean) -> Unit,
    onDirectoryChosen: (directory: Uri) -> Unit,
    onGestureNameChanged: (gestureName: String) -> Unit,
    onStartRecordingPressed: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${if (state.camera.isCurrentFrontFacing) "Front 🤳" else "Back 📸"} camera",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.size(12.dp))
            if (state.camera is AllTypes) {
                Switch(
                    checked = state.camera.isCurrentFrontFacing,
                    onCheckedChange = onCameraToggle
                )
            }
        }
        var gestureName by remember { mutableStateOf("") }
        val pickPathLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
            onResult = { uri ->
                if (uri != null) {
                    onDirectoryChosen(uri)
                }
            }
        )
        Spacer(modifier = Modifier.size(12.dp))
        Button(onClick = {
            pickPathLauncher.launch(null)
        }) {
            val action = if (state is ReadyToStartRecording)
                "Change"
            else
                "Choose"
            Text("$action directory to save CSVs", fontSize = 25.sp)
        }
        Spacer(modifier = Modifier.size(12.dp))
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
        Spacer(modifier = Modifier.size(12.dp))
        if (state is ReadyToStartRecording) {
            Button(
                onClick = onStartRecordingPressed
            ) {
                Text("Start Recording", fontSize = 25.sp)
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                "Gesture-directory path: ${state.directoryUri.path}",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                "Gesture will be recorded in files: ${state.gestureName}_N.csv",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
internal fun Pausable(
    state: State.LiveCamera.Recording.Pausable,
    onPauseRecordingPressed: () -> Unit,
    onResumeRecordingPressed: () -> Unit,
    onStopRecordingPressed: () -> Unit,
) {
    Row {
        Button(
            onClick = if (state.isPaused) onResumeRecordingPressed else onPauseRecordingPressed
        ) {
            Text(
                text = if (state.isPaused) "▶️Resume" else "⏸️Pause",
                fontSize = 25.sp
            )
        }
        Button(
            onClick = onStopRecordingPressed
        ) {
            Text("⏹️Stop", fontSize = 25.sp)
        }
    }
    when (state) {
        is State.LiveCamera.Recording.Pausable.PreparingForTheNextRecording -> {
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

        is State.LiveCamera.Recording.Pausable.RecordingMotion -> {
            Text(
                "Gesture ${state.gestureName} #: ${state.gestureNum}",
                fontSize = 20.sp
            )
            Text("\uD83D\uDD34 Live: Recording", fontSize = 23.sp)
            Text("Time seconds left: ${state.timeLeft}", fontSize = 25.sp)
        }
    }
}

@Composable
internal fun SavingPreviousMotion(
    state: State.LiveCamera.Recording.SavingPreviousMotion,
) {
    Text("💾 Saving...", fontSize = 25.sp)
    Text("Done: ${state.savingProgress}%", fontSize = 25.sp)
    Text(
        "Gesture ${state.gestureName} #: ${state.gestureNum}",
        fontSize = 20.sp
    )
}
