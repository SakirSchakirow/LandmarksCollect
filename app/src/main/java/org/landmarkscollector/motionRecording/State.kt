package org.landmarkscollector.motionRecording

import android.net.Uri
import org.landmarkscollector.data.LandmarksRecording
import org.landmarkscollector.motionRecording.CamerasInfo.CamerasAvailable

internal sealed interface State {

    object NoCameraPermitted : State

    sealed class LiveCamera : State {

        fun configuration() = Triple(isOneHandGesture, isGpuEnabled, camera.isCurrentFrontFacing)

        abstract val isOneHandGesture: Boolean

        abstract val isGpuEnabled: Boolean

        abstract val camera: CamerasAvailable

        abstract fun copy(
            isOneHandGesture: Boolean = this.isOneHandGesture,
            isGpuEnabled: Boolean = this.isGpuEnabled,
            camera: CamerasAvailable = this.camera,
        ): LiveCamera

        sealed class Steady : LiveCamera() {

            data class WaitingForDirectoryAndGesture(
                val directoryUri: Uri? = null,
                val gestureName: String? = null,
                override val isOneHandGesture: Boolean = false,
                override val isGpuEnabled: Boolean = false,
                override val camera: CamerasAvailable,
            ) : Steady() {

                override fun copy(
                    isOneHandGesture: Boolean,
                    isGpuEnabled: Boolean,
                    camera: CamerasAvailable,
                ): LiveCamera = copy(
                    gestureName = gestureName,
                    isOneHandGesture = isOneHandGesture,
                    isGpuEnabled = isGpuEnabled,
                    camera = camera
                )
            }

            data class ReadyToStartRecording(
                val directoryUri: Uri,
                val gestureName: String,
                override val isOneHandGesture: Boolean,
                override val isGpuEnabled: Boolean,
                override val camera: CamerasAvailable,
            ) : Steady() {

                override fun copy(
                    isOneHandGesture: Boolean,
                    isGpuEnabled: Boolean,
                    camera: CamerasAvailable,
                ): LiveCamera = copy(
                    directoryUri = directoryUri,
                    isOneHandGesture = isOneHandGesture,
                    isGpuEnabled = isGpuEnabled,
                    camera = camera
                )
            }
        }

        sealed class Recording : LiveCamera() {
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
                    override val isOneHandGesture: Boolean,
                    override val isGpuEnabled: Boolean,
                    override val camera: CamerasAvailable,
                ) : Pausable() {

                    override fun copy(
                        isOneHandGesture: Boolean,
                        isGpuEnabled: Boolean,
                        camera: CamerasAvailable,
                    ): LiveCamera = copy(
                        directoryUri = directoryUri,
                        isOneHandGesture = isOneHandGesture,
                        isGpuEnabled = isGpuEnabled,
                        camera = camera
                    )
                }

                data class RecordingMotion(
                    override val directoryUri: Uri,
                    override val gestureName: String,
                    override val gestureNum: UInt = 1u,
                    override val isPaused: Boolean = false,
                    val timeLeft: UInt = MAX_FRAMES_SECS,
                    val hands: LandmarksRecording = LandmarksRecording(),
                    val facePose: LandmarksRecording = LandmarksRecording(),
                    override val isOneHandGesture: Boolean,
                    override val isGpuEnabled: Boolean,
                    override val camera: CamerasAvailable,
                ) : Pausable() {

                    override fun copy(
                        isOneHandGesture: Boolean,
                        isGpuEnabled: Boolean,
                        camera: CamerasAvailable,
                    ): LiveCamera = copy(
                        directoryUri = directoryUri,
                        isOneHandGesture = isOneHandGesture,
                        isGpuEnabled = isGpuEnabled,
                        camera = camera
                    )
                }
            }

            data class SavingPreviousMotion(
                override val directoryUri: Uri,
                override val gestureName: String,
                override val gestureNum: UInt,
                val savingProgress: UInt = UInt.MIN_VALUE,
                override val isOneHandGesture: Boolean,
                override val isGpuEnabled: Boolean,
                override val camera: CamerasAvailable,
            ) : Recording() {

                override fun copy(
                    isOneHandGesture: Boolean,
                    isGpuEnabled: Boolean,
                    camera: CamerasAvailable,
                ): LiveCamera = copy(
                    directoryUri = directoryUri,
                    isOneHandGesture = isOneHandGesture,
                    isGpuEnabled = isGpuEnabled,
                    camera = camera
                )
            }
        }
    }

    companion object {

        const val DELAY_SECS: UInt = 3u
        const val MAX_FRAMES_SECS: UInt = 5u
        const val TOTAL_GESTURES_NUM: UInt = 10u
    }
}
