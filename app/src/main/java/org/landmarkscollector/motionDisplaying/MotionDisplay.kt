package org.landmarkscollector.motionDisplaying

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@Composable
fun MotionDisplay() {
    val context = LocalContext.current

    val overlayView: OverlayView = remember {
        OverlayView(context)
    }
    val graphicOverlay: GraphicOverlay = remember {
        GraphicOverlay(context)
    }

    var frames: List<Frame> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(frames) {
        launch {
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
                delay(24)
                with(overlayView) {
                    setResults(
                        handLandmarks = hands,
                        imageHeight = 480,
                        imageWidth = 640,
                        runningMode = RunningMode.VIDEO
                    )
                    invalidate()
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
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
                arrayOf("text/csv")
            )
        }) {
            Text("Set Csv", fontSize = 25.sp)
        }
    }
}
