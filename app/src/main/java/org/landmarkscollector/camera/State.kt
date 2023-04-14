package org.landmarkscollector.camera

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

    sealed interface Recording : State {
        val directoryUri: Uri
        val gestureName: String
        val gestureNum: UInt

        sealed interface Pausable : Recording {

            val isPaused: Boolean

            data class PreparingForTheNextRecording(
                override val directoryUri: Uri,
                override val gestureName: String,
                override val gestureNum: UInt,
                override val isPaused: Boolean = false,
                val delayTicks: UInt = DELAY_SECS
            ) : Pausable

            data class RecordingMotion(
                override val directoryUri: Uri,
                override val gestureName: String,
                override val gestureNum: UInt = 1u,
                override val isPaused: Boolean = false,
                val timeLeft: UInt = MAX_FRAMES_SECS,
                val hands: LandmarksRecording = LandmarksRecording(),
                val facePose: LandmarksRecording = LandmarksRecording()
            ) : Pausable
        }

        data class SavingPreviousMotion(
            override val directoryUri: Uri,
            override val gestureName: String,
            override val gestureNum: UInt,
            val savingProgress: UInt = UInt.MIN_VALUE
        ) : Recording
    }

    companion object {

        const val DELAY_SECS: UInt = 3u
        const val MAX_FRAMES_SECS: UInt = 5u
        const val TOTAL_GESTURES_NUM: UInt = 10u
    }
}
