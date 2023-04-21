package org.landmarkscollector.motionRecording

import android.net.Uri
import org.landmarkscollector.data.Landmark.Hand
import org.landmarkscollector.mlkit.detectors.DetectorResult

internal sealed interface Event {

    sealed interface Ui : Event {
        class OnCamerasInfoReceived(val camerasInfo: CamerasInfo) : Ui

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

        class OnFacePoseResults(val result: DetectorResult) : Ui

        object OnResumeRecording : Ui

        object OnPauseRecording : Ui

        object OnStopRecording : Ui

        object OnToggleCamera : Ui

        object OnToggleHandsCount : Ui

        object OnToggleCpuGpu : Ui
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
