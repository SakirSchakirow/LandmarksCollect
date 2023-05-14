package org.landmarkscollector.motionDisplaying

import android.content.Context
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.landmarkscollector.data.Frame
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.domain.repository.csv.CsvFramesReader
import org.landmarkscollector.hands.OverlayView
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.face.FaceMeshGraphic
import org.landmarkscollector.mlkit.pose.PoseGraphic
import org.landmarkscollector.ui.components.RatesColumn
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@Composable
fun MotionDisplay() {
    val context = LocalContext.current

    val interpreter = Interpreter(loadModelFile(context))

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

    var framesOfCsv: List<Frame> by remember { mutableStateOf(emptyList()) }

    var gestureDetectionResults: Map<String, Float> by remember {
        mutableStateOf(emptyMap())
    }

    var previousMotionAnimation: Job? = null

    LaunchedEffect(framesOfCsv) {
        previousMotionAnimation?.cancelAndJoin()
        previousMotionAnimation = launch {
            if (isActive) {
                val frames = framesOfCsv
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
                            framesOfCsv = reader.readFrames(it)

                            framesOfCsv.toGestureTensorOrNull()
                                ?.let { tensor ->
                                    val output: Array<FloatArray> = arrayOf(
                                        floatArrayOf(
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f,
                                            0.0f
                                        )
                                    )

                                    interpreter.run(arrayOf(tensor), output)

                                    val result = output[0]

                                    val labels = listOf(
                                        "день",
                                        "здравствуйте",
                                        "нормально",
                                        "спасибо",
                                        "я",
                                        "дела",
                                        "добрый",
                                        "пожалуйста",
                                        "хорошо",
                                        "до свидания",
                                        "добрый день",
                                    )

                                    gestureDetectionResults = buildMap {
                                        result.mapIndexed { index, prob ->
                                            put(labels[index], prob)
                                        }
                                    }
                                }
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
        RatesColumn(gestureDetectionResults)
    }
}

private fun List<Frame>.toGestureTensorOrNull(): Array<FloatArray>? {
    // (30, 1086)
    val frames = take(30)
        .map { frame ->
            frame.landmarks.map { mark ->
                listOf(mark?.x ?: 0f, mark?.y ?: 0f)
            }.flatten()
        }.map(List<Float>::toFloatArray)
        .toTypedArray()
    return if (frames.size == 30 && frames.all { row -> row.size == 1086 }) frames else null
}

@Throws(IOException::class)
private fun loadModelFile(context: Context): MappedByteBuffer {
    val MODEL_ASSETS_PATH = "gesturesModel.tflite"
    val assetFileDescriptor = context.assets.openFd(MODEL_ASSETS_PATH)
    return FileInputStream(assetFileDescriptor.fileDescriptor)
        .channel
        .map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
}
