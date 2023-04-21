package org.landmarkscollector

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.flow.StateFlow
import org.landmarkscollector.data.CsvRow
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.data.Landmark.Face
import org.landmarkscollector.data.Landmark.Hand.Left
import org.landmarkscollector.data.Landmark.Hand.Right
import org.landmarkscollector.data.Landmark.Pose
import org.landmarkscollector.mlkit.detectors.DetectorResult

class CameraScreenViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_GESTURE = "KEY_GESTURE"
    }

    private val csvRows: List<CsvRow>? = null
    val currentGesture: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_GESTURE, null)
    fun setGestureName(name: String) {
        savedStateHandle[KEY_GESTURE] = name
    }

    fun onFacePoseResults(result: DetectorResult) {
        val poseLandmarks = result.pose.allPoseLandmarks
            .map(::toLandmark)
        val faceLandmarks = result.faces.firstOrNull()
            ?.allPoints
            ?.map(::toLandmark)

        Log.d("OnMediapipePose: ", "Count: ${poseLandmarks.size}")
        Log.d("OnMediapipeFace: ", "Count: ${faceLandmarks?.size ?: 0}")
    }

    fun onHandResults(handLandmarkerResult: HandLandmarkerResult) {
        val landmarks = handLandmarkerResult.landmarks()
        val handednesses = handLandmarkerResult.handednesses()
        landmarks.mapIndexed { index, normalizedLandmarks ->
            val handedness = handednesses.getOrNull(index)?.firstOrNull()
            requireNotNull(handedness) { "Both info on landmarks and handedness should be available" }
            val marks = normalizedLandmarks.getLandmarks(handedness.categoryName() == "Right")
            Log.d("OnMediapipeHand: ", marks.toString())
        }
    }

    private fun toLandmark(poseLandmark: PoseLandmark) {
        with(poseLandmark.position3D) {
            Pose(poseLandmark.landmarkType, x, y, z)
        }
    }

    private fun toLandmark(faceMeshPoint: FaceMeshPoint): Landmark {
        return with(faceMeshPoint.position) {
            Face(faceMeshPoint.index, x, y, z)
        }
    }

    private fun List<NormalizedLandmark>.getLandmarks(isRightHand: Boolean): List<Landmark> =
        mapIndexed { index, normalizedLandmark ->
            normalizedLandmark.toLandmark(
                index,
                isRightHand
            )
        }

    private fun NormalizedLandmark.toLandmark(index: Int, isRightHand: Boolean): Landmark =
        with(this) {
            if (isRightHand) {
                Right(index, x(), y(), z())
            } else {
                Left(index, x(), y(), z())
            }
        }

    private fun Landmark.toCsvRow(frame: Int): CsvRow {
        return CsvRow(
            frame = frame,
            landmarkIndex = landmarkIndex,
            type = landmarkType,
            x = x,
            y = y,
            z = z
        )
    }
}
