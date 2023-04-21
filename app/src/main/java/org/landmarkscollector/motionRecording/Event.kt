package org.landmarkscollector.motionRecording

import android.net.Uri
import androidx.camera.core.ImageProxy
import org.landmarkscollector.data.Landmark.Hand
import org.landmarkscollector.mlkit.detectors.DetectorResult

sealed interface Event {

    sealed interface Ui : Event {
        class OnDirectoryChosen(
            val directory: Uri,
        ) : Ui

        class OnGestureNameChanged(
            val gestureName: String,
        ) : Ui

        object OnStartRecordingPressed : Ui

        class OnHandResults(
            val handsResults: List<List<Hand>>,
        ) : Ui

        class OnFacePoseResults(
            val imageProxy: ImageProxy, val result: DetectorResult,
        ) : Ui

        object OnResumeRecording : Ui

        object OnPauseRecording : Ui

        object OnStopRecording : Ui

        object OnToggleCamera : Ui
    }

    sealed interface Internal : Event {

        class RecordIsSaving(
            val savingProgress: UInt = UInt.MIN_VALUE,
        ) : Internal

        class RecordingTimeLeft(val timeLeft: UInt) : Internal

        object RecordingSaved : Internal

        class WaitingForTheNextMotionRecording(val delayTicks: UInt) : Internal
    }
}
