package org.landmarkscollector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
        private const val KEY_IS_RECORDING_ON = "KEY_IS_RECORDING_ON"
        private const val KEY_GESTURE_NUM = "KEY_GESTURE_NUM"
    }

    private var facePoseFrame = 0
    private val csvRowsFacePose = HashMap<String, CsvRow>()
    private var handsFrame = 0
    private val csvRowsHands = HashMap<String, CsvRow>()

    val currentGesture: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_GESTURE, null)

    fun setGestureName(name: String) {
        savedStateHandle[KEY_GESTURE] = name
    }

    val isRecordingOn: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_IS_RECORDING_ON, false)

    val isRecordingButtonDisabled = isRecordingOn.combine(currentGesture) { isOn, gesture ->
        isOn || gesture.isNullOrBlank()
    }

    val gesturesNum: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_GESTURE_NUM, 0)
    fun setGesturesNum(gesturesNum: Int) {
        savedStateHandle[KEY_GESTURE_NUM] = gesturesNum
        //save csv file
        saveCsvFile()
    }

    fun setIsRecordingOn(isOn: Boolean) {
        savedStateHandle[KEY_IS_RECORDING_ON] = isOn
    }

    fun onFacePoseResults(result: DetectorResult) {
        if (isRecordingOn.value) {
            val poseLandmarks = result.pose.allPoseLandmarks
                .map(::toLandmark)
            val faceLandmarks = result.faces.firstOrNull()
                ?.allPoints
                ?.map(::toLandmark)

            val frame = facePoseFrame++
            csvRowsFacePose.putAll(
                poseLandmarks.map { landmark ->
                    landmark.toCsvRow(frame)
                }.associateBy(CsvRow::rowId)
            )
            faceLandmarks?.map { landmark ->
                landmark.toCsvRow(frame)
            }?.associateBy(CsvRow::rowId)
                ?.let(csvRowsFacePose::putAll)
        }
    }

    fun onHandResults(handLandmarkerResult: HandLandmarkerResult) {
        if (isRecordingOn.value) {
            val landmarks = handLandmarkerResult.landmarks()
            val handednesses = handLandmarkerResult.handednesses()
            landmarks.mapIndexed { index, normalizedLandmarks ->
                val handedness = handednesses.getOrNull(index)?.firstOrNull()
                requireNotNull(handedness) { "Both info on landmarks and handedness should be available" }
                val marks = normalizedLandmarks.getLandmarks(handedness.categoryName() == "Right")

                val frame = handsFrame++
                csvRowsHands.putAll(
                    marks.map { mark ->
                        mark.toCsvRow(frame)
                    }.associateBy(CsvRow::rowId)
                )
            }
        }
    }

    private fun toLandmark(poseLandmark: PoseLandmark): Landmark {
        return with(poseLandmark.position3D) {
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

    private fun saveCsvFile() {
        //TODO
    }
}
