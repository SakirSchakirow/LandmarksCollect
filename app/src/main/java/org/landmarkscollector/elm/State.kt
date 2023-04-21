package org.landmarkscollector.elm

import android.net.Uri
import org.landmarkscollector.data.LandmarksRecording

sealed interface State {

    sealed interface Steady : State {

        data class WaitingForDirectoryAndGesture(
            val directoryUri: Uri? = null,
            val gestureName: String? = null
        ) : Steady

        class ReadyToStartRecording(
            val directoryUri: Uri,
            val gestureName: String
        ) : Steady
    }

    sealed class Recording : State {
        abstract val directoryUri: Uri
        abstract val gestureName: String
        abstract val gestureNum: UInt

        data class PreparingForTheNextRecording(
            override val directoryUri: Uri,
            override val gestureName: String,
            override val gestureNum: UInt,
            val delayTicks: UInt = DELAY_SECS
        ) : Recording()

        data class SavingPreviousMotion(
            override val directoryUri: Uri,
            override val gestureName: String,
            override val gestureNum: UInt,
            val savingProgress: UInt = UInt.MIN_VALUE
        ) : Recording()

        data class RecordingMotion(
            override val directoryUri: Uri,
            override val gestureName: String,
            override val gestureNum: UInt = 1u,
            val timeLeft: UInt = MAX_FRAMES_SECS,
            val hands: LandmarksRecording = LandmarksRecording(),
            val facePose: LandmarksRecording = LandmarksRecording()
        ) : Recording()
    }

    companion object {

        const val DELAY_SECS: UInt = 3u
        const val MAX_FRAMES_SECS: UInt = 5u
        const val TOTAL_GESTURES_NUM: UInt = 10u
    }
}
