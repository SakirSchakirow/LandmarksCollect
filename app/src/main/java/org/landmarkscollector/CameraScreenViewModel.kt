package org.landmarkscollector

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import org.landmarkscollector.data.CsvRow
import org.landmarkscollector.data.CsvRow.Companion.empty
import org.landmarkscollector.data.CsvRow.Companion.rowId
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.data.Landmark.*
import org.landmarkscollector.data.Landmark.Hand.Left
import org.landmarkscollector.data.Landmark.Hand.Right
import org.landmarkscollector.domain.repository.DataConverter
import org.landmarkscollector.domain.repository.ExportRepository
import org.landmarkscollector.domain.repository.ExportRepositoryImpl
import org.landmarkscollector.domain.repository.csv.DataConverterCSV
import org.landmarkscollector.domain.repository.file.AndroidInternalStorageFileWriter
import org.landmarkscollector.mlkit.detectors.DetectorResult
import java.lang.Integer.min

class CameraScreenViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_GESTURE = "KEY_GESTURE"
        private const val KEY_DIRECTORY = "KEY_DIRECTORY"
        private const val KEY_IS_RECORDING_ON = "KEY_IS_RECORDING_ON"
        private const val KEY_GESTURE_NUM = "KEY_GESTURE_NUM"
        private const val CSV_MIME_TYPE = "text/csv"
    }

    private val dataConverter: DataConverter = DataConverterCSV()

    private var facePoseFrames = UInt.MIN_VALUE
    private val csvRowsFacePose = HashMap<String, CsvRow>()

    private var handsFrames = UInt.MIN_VALUE
    private val csvRowsHands = HashMap<String, CsvRow>()

    val currentGestureName: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_GESTURE, null)

    fun setGestureName(name: String) {
        savedStateHandle[KEY_GESTURE] = name
    }

    val currentDirectory: StateFlow<Uri?> = savedStateHandle.getStateFlow(KEY_DIRECTORY, null)

    fun setCurrentDirectory(uri: Uri) {
        savedStateHandle[KEY_DIRECTORY] = uri
    }

    val isRecordingOn: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_IS_RECORDING_ON, false)

    val isRecordingButtonDisabled = isRecordingOn.combine(currentGestureName) { isOn, gesture ->
        isOn || gesture.isNullOrBlank()
    }.combine(currentDirectory) { isDisabled, directory ->
        isDisabled || directory == null
    }

    val gesturesNum: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_GESTURE_NUM, 0)
    fun setGesturesNum(context: Context, gesturesNum: Int) {
        savedStateHandle[KEY_GESTURE_NUM] = gesturesNum
        saveCsvFile(context)
    }

    fun setIsRecordingOn(isOn: Boolean) {
        savedStateHandle[KEY_IS_RECORDING_ON] = isOn
    }

    fun onFacePoseResults(imageProxy: ImageProxy, result: DetectorResult) {
        if (isRecordingOn.value) {
            val poseLandmarks = result.pose.allPoseLandmarks
                .map { toLandmark(imageProxy, it) }
            val faceLandmarks = result.faces.firstOrNull()
                ?.allPoints
                ?.map { toLandmark(imageProxy, it) }

            val frame = facePoseFrames++
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
            val frame = handsFrames++
            val landmarks = handLandmarkerResult.landmarks()
            val handednesses = handLandmarkerResult.handednesses()

            landmarks.mapIndexed { index, normalizedLandmarks ->
                val handedness = handednesses.getOrNull(index)?.firstOrNull()
                requireNotNull(handedness) { "Both info on landmarks and handedness should be available" }
                val marks = normalizedLandmarks.getLandmarks(handedness.categoryName() == "Right")

                csvRowsHands.putAll(
                    marks.map { mark ->
                        mark.toCsvRow(frame)
                    }.associateBy(CsvRow::rowId)
                )
            }
        }
    }

    private fun toLandmark(imageProxy: ImageProxy, poseLandmark: PoseLandmark): Landmark {
        return with(poseLandmark.position3D) {
            val normalizedX = x / imageProxy.width
            val normalizedY = y / imageProxy.height
            //the magnitude of z is roughly the same as x
            // thus, we consider normalized-z preserve the same ratio
            val normalizedZ = normalizedX * x / z
            Pose(
                landmarkIndex = poseLandmark.landmarkType.toUInt(),
                x = normalizedX,
                y = normalizedY,
                z = normalizedZ
            )
        }
    }

    private fun toLandmark(imageProxy: ImageProxy, faceMeshPoint: FaceMeshPoint): Landmark {
        return with(faceMeshPoint.position) {
            val normalizedX = x / imageProxy.width
            val normalizedY = y / imageProxy.height
            //the unit of measure for the z is the same as x and Y
            // thus, we consider normalized-z preserve the same ratio
            val normalizedZ = normalizedX * x / z
            Face(
                landmarkIndex = faceMeshPoint.index.toUInt(),
                x = normalizedX,
                y = normalizedY,
                z = normalizedZ
            )
        }
    }

    private fun List<NormalizedLandmark>.getLandmarks(isRightHand: Boolean): List<Landmark> =
        mapIndexed { index, normalizedLandmark ->
            normalizedLandmark.toLandmark(
                index.toUInt(),
                isRightHand
            )
        }

    private fun NormalizedLandmark.toLandmark(index: UInt, isRightHand: Boolean): Landmark =
        with(this) {
            if (isRightHand) {
                Right(index, x(), y(), z())
            } else {
                Left(index, x(), y(), z())
            }
        }

    private fun Landmark.toCsvRow(frame: UInt): CsvRow {
        return CsvRow(
            frame = frame,
            landmark = this
        )
    }

    private fun saveCsvFile(
        context: Context
    ) {
        val currentDirectoryUri = currentDirectory.value
        require(currentDirectoryUri != null) { "Set directory before saving a csv-file" }

        val futureCsvFile = DocumentFile.fromTreeUri(context, currentDirectoryUri)!!
            .createFile(CSV_MIME_TYPE, "${currentGestureName.value}_${gesturesNum.value}")!!

        val exportRepository: ExportRepository =
            ExportRepositoryImpl(AndroidInternalStorageFileWriter(context), dataConverter)

        exportRepository.startExportData(
            exportList = getFulfilledFramesCsvRows(
                facesPoses = csvRowsFacePose,
                facesPosesNumber = facePoseFrames,
                hands = csvRowsHands,
                handsNumber = handsFrames
            ),
            futureCsvFile = futureCsvFile.uri
        ).launchIn(viewModelScope)
        csvRowsFacePose.clear()
        facePoseFrames = UInt.MIN_VALUE
        csvRowsHands.clear()
        handsFrames = UInt.MIN_VALUE
    }

    private fun getFulfilledFramesCsvRows(
        facesPoses: Map<String, CsvRow>,
        facesPosesNumber: UInt,
        hands: Map<String, CsvRow>,
        handsNumber: UInt,
    ): List<CsvRow> {
        val totalFramesNumber = min(facesPosesNumber.toInt(), handsNumber.toInt()).toUInt()
        return buildList {
            for (frameNumber in UInt.MIN_VALUE until totalFramesNumber) {
                addLandmarks(frameNumber, facesPoses, LandmarkType.Face)
                addLandmarks(frameNumber, hands, LandmarkType.RightHand)
                addLandmarks(frameNumber, hands, LandmarkType.LeftHand)
                addLandmarks(frameNumber, facesPoses, LandmarkType.Pose)
            }
        }
    }

    private fun MutableList<CsvRow>.addLandmarks(
        frameNumber: UInt,
        rows: Map<String, CsvRow>,
        landmarkType: LandmarkType
    ) {
        for (landmarkIndex in UInt.MIN_VALUE until landmarkType.totalLandmarkNumber) {
            val rowId = landmarkType.rowId(frameNumber, landmarkIndex)
            val csvRow = rows[rowId] ?: empty(frameNumber, landmarkIndex, landmarkType)
            add(csvRow)
        }
    }
}
