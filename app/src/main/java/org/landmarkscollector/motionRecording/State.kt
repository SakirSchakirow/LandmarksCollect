package org.landmarkscollector.motionRecording

import android.net.Uri
import org.landmarkscollector.data.LandmarksRecording

sealed class State {

    abstract val isFrontCamera: Boolean

    sealed class Steady : State() {

        data class WaitingForDirectoryAndGesture(
            val directoryUri: Uri? = null,
            val gestureName: String? = null,
            override val isFrontCamera: Boolean = true,
        ) : Steady()

        data class ReadyToStartRecording(
            val directoryUri: Uri,
            val gestureName: String,
            override val isFrontCamera: Boolean = true,
        ) : Steady()
    }

    sealed class Recording : State() {
        abstract val directoryUri: Uri
        abstract val gestureName: String
        abstract val gestureNum: UInt

        sealed class Pausable : Recording() {

            abstract val isPaused: Boolean

            data class PreparingForTheNextRecording(
                override val directoryUri: Uri,
                override val gestureName: String,
                override val gestureNum: UInt,
                override val isPaused: Boolean = false,
                val delayTicks: UInt = DELAY_SECS,
                override val isFrontCamera: Boolean,
            ) : Pausable()

            data class RecordingMotion(
                override val directoryUri: Uri,
                override val gestureName: String,
                override val gestureNum: UInt = 1u,
                override val isPaused: Boolean = false,
                val timeLeft: UInt = MAX_FRAMES_SECS,
                val hands: LandmarksRecording = LandmarksRecording(),
                val facePose: LandmarksRecording = LandmarksRecording(),
                override val isFrontCamera: Boolean,
            ) : Pausable()
        }

        data class SavingPreviousMotion(
            override val directoryUri: Uri,
            override val gestureName: String,
            override val gestureNum: UInt,
            val savingProgress: UInt = UInt.MIN_VALUE,
            override val isFrontCamera: Boolean,
        ) : Recording()
    }

    companion object {

        const val DELAY_SECS: UInt = 3u
        const val MAX_FRAMES_SECS: UInt = 5u
        const val TOTAL_GESTURES_NUM: UInt = 10u
    }
}
