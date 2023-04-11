package org.landmarkscollector.elm

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.landmarkscollector.mlkit.detectors.DetectorResult

sealed interface Event {

    sealed interface Ui : Event {
        class OnDirectoryChosen(
            val directory: Uri
        ) : Ui

        class OnGestureNameChanged(
            val gestureName: String
        ) : Ui

        class OnStartRecordingPressed(val context: Context) : Ui

        class OnHandResults(
            val results: HandLandmarkerResult
        ) : Ui

        class OnFacePoseResults(
            val imageProxy: ImageProxy, val result: DetectorResult
        ) : Ui
    }

    sealed interface Internal : Event {

        class RecordIsSaving(
            val savingProgress: UInt = UInt.MIN_VALUE
        ) : Internal

        class RecordingTimeLeft(val timeLeft: UInt) : Internal

        object RecordingSaved : Internal

        class WaitingForTheNextMotionRecording(val delayTicks: UInt) : Internal
    }
}
