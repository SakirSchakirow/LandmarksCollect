package org.landmarkscollector.motionDisplaying

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.landmarkscollector.data.Frame
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.domain.repository.csv.CsvFramesReader
import org.landmarkscollector.hands.OverlayView
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.face.FaceMeshGraphic
import org.landmarkscollector.mlkit.pose.PoseGraphic

@Composable
fun MotionDisplay() {
    val context = LocalContext.current

    val overlayView: OverlayView = remember {
        OverlayView(context)
    }
    val graphicOverlay: GraphicOverlay = remember {
        GraphicOverlay(context).apply {
            setImageSourceInfo(
                480,
                640,
                true
            )
        }
    }


    var frames: List<Frame> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(frames) {
        launch {
            repeat(100) {
                for (frameIndex in frames.indices) {
                    val hands = buildList {
                        with(frames[frameIndex].landmarks) {
                            filterIsInstance(Landmark.Hand.Right::class.java)
                                .takeIf { it.isNotEmpty() }
                                ?.let { add(it) }
                            filterIsInstance(Landmark.Hand.Left::class.java)
                                .takeIf { it.isNotEmpty() }
                                ?.let { add(it) }
                        }
                    }

                    val poseLandmarks = frames[frameIndex].landmarks
                        .filterIsInstance(Landmark.Pose::class.java)
                        .takeIf { it.isNotEmpty() }

                    val faceLandmarks = frames[frameIndex].landmarks
                        .filterIsInstance(Landmark.Face::class.java)
                        .takeIf { it.isNotEmpty() }

                    delay(100)
                    with(overlayView) {
                        setResults(handLandmarks = hands, 640, 480, RunningMode.LIVE_STREAM)
                        invalidate()
                    }
                    with(graphicOverlay) {
                        clear()
                        poseLandmarks?.let {
                            add(
                                PoseGraphic(
                                    overlay = this,
                                    poseLandmarks = poseLandmarks,
                                    640,
                                    480,
                                )
                            )
                        }
                        faceLandmarks?.let {
                            add(
                                FaceMeshGraphic(
                                    overlay = this,
                                    points = faceLandmarks,
                                    640,
                                    480,
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { graphicOverlay },
            modifier = Modifier.fillMaxSize()
        )
        AndroidView(
            factory = { overlayView },
            modifier = Modifier.fillMaxSize()
        )

        val reader = CsvFramesReader()
        val pickFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                if (uri != null) {
                    context.contentResolver
                        .openInputStream(uri)!!
                        .use {
                            frames = reader.readFrames(it)
                        }
                }
            }
        )
        Button(onClick = {
            pickFileLauncher.launch(
                arrayOf("text/*")
            )
        }) {
            Text("Set Csv", fontSize = 25.sp)
        }
    }
}
