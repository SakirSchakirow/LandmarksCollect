package org.landmarkscollector

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.StateFlow
import org.landmarkscollector.data.CsvRow
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.data.Landmark.Hand.Left
import org.landmarkscollector.data.Landmark.Hand.Right

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
}
