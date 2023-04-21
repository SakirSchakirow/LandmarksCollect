package org.landmarkscollector

import android.Manifest
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import org.landmarkscollector.mlkit.posedetector.PoseDetectorProcessor
import org.landmarkscollector.hands.HandLandmarkerHelper
import org.landmarkscollector.hands.LandmarkerListener
import org.landmarkscollector.hands.OverlayView
import org.landmarkscollector.hands.ResultBundle
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.VisionImageProcessor
import org.landmarkscollector.mlkit.facemeshdetector.FaceMeshDetectorProcessor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission state
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
            lateinit var imageProcessors: List<VisionImageProcessor>
            val backgroundExecutor = Executors.newSingleThreadExecutor()
            backgroundExecutor.execute {
                handLandmarkerHelper = HandLandmarkerHelper(context, object : LandmarkerListener {

                    override fun onError(error: String) {
                        Log.e("CameraScreen", error)
                    }

                    override fun onResults(resultBundle: ResultBundle) {
                        ContextCompat.getMainExecutor(context).execute {
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

            imageProcessors = listOf(
                FaceMeshDetectorProcessor(context),
                PoseDetectorProcessor(
                    context = context,
                    //TODO change config
                    options = AccuratePoseDetectorOptions.Builder()//PoseDetectorOptions.Builder()
                        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)//PoseDetectorOptions.STREAM_MODE)
                        .setPreferredHardwareConfigs(AccuratePoseDetectorOptions.CPU_GPU)//PoseDetectorOptions.CPU_GPU)
                        .build(),
                    visualizeZ = true,
                    rescaleZForVisualization = true
                )
            )

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
                                /*
                                handLandmarkerHelper.detectLiveStream(
                                    imageProxy = imageProxy,
                                    isFrontCamera = cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA
                                )
                                 */
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
                                    imageProcessors.first()
                                        .processImageProxy(imageProxy, graphicOverlay)
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
        }
    }
}
