package org.landmarkscollector.motionRecording

import org.landmarkscollector.data.FrameLandmark
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.data.RowId
import org.landmarkscollector.domain.repository.FileCreator
import org.landmarkscollector.motionRecording.CamerasInfo.CamerasAvailable.AllTypes
import org.landmarkscollector.motionRecording.Command.PrepareForGestureRecording
import org.landmarkscollector.motionRecording.Command.SaveRecording
import org.landmarkscollector.motionRecording.Command.StartRecording
import org.landmarkscollector.motionRecording.Event.Ui
import org.landmarkscollector.motionRecording.Event.Internal
import org.landmarkscollector.motionRecording.Event.Internal.GesturesRate
import org.landmarkscollector.motionRecording.Event.Internal.RecordIsSaving
import org.landmarkscollector.motionRecording.Event.Internal.RecordingSaved
import org.landmarkscollector.motionRecording.Event.Internal.RecordingTimeLeft
import org.landmarkscollector.motionRecording.Event.Internal.WaitingForTheNextMotionRecording
import org.landmarkscollector.motionRecording.State.*
import org.landmarkscollector.motionRecording.State.Companion.TOTAL_GESTURES_NUM
import org.landmarkscollector.motionRecording.State.LiveCamera.*
import org.landmarkscollector.motionRecording.State.LiveCamera.Recording.Pausable
import org.landmarkscollector.motionRecording.State.LiveCamera.Recording.Pausable.PreparingForTheNextRecording
import org.landmarkscollector.motionRecording.State.LiveCamera.Recording.Pausable.RecordingMotion
import org.landmarkscollector.motionRecording.State.LiveCamera.Recording.SavingPreviousMotion
import org.landmarkscollector.motionRecording.State.LiveCamera.Steady.*
import org.landmarkscollector.motionRecording.State.LiveCamera.Steady.WaitingForDirectoryAndGesture.Companion.DETECTION_FRAME_BUFFER_SIZE
import vivid.money.elmslie.core.store.dsl_reducer.ScreenDslReducer

internal class Reducer(
    private val fileCreator: FileCreator,
) : ScreenDslReducer<Event, Ui, Internal, State, Effect, Command>(
    uiEventClass = Ui::class,
    internalEventClass = Internal::class
) {

    override fun Result.internal(event: Internal) {
        val currentState = state
        when (event) {
            is RecordIsSaving -> if (currentState is SavingPreviousMotion) {
                if (event.savingProgress <= 100u) {
                    state {
                        currentState.copy(savingProgress = event.savingProgress)
                    }
                }
            }

            is RecordingTimeLeft -> if (currentState is RecordingMotion) {
                if (event.timeLeft == UInt.MIN_VALUE) {
                    state {
                        SavingPreviousMotion(
                            directoryUri = currentState.directoryUri,
                            gestureName = currentState.gestureName,
                            gestureNum = currentState.gestureNum,
                            isOneHandGesture = currentState.isOneHandGesture,
                            isGpuEnabled = currentState.isGpuEnabled,
                            camera = currentState.camera
                        )
                    }

                    commands {
                        +SaveRecording(
                            uri = fileCreator.createFile(
                                currentState.directoryUri,
                                currentState.gestureName,
                                currentState.gestureNum
                            ),
                            hands = currentState.hands,
                            facePose = currentState.facePose
                        )
                    }
                } else {
                    state {
                        currentState.copy(
                            timeLeft = event.timeLeft
                        )
                    }
                }
            }

            RecordingSaved -> if (currentState is SavingPreviousMotion) {
                state {
                    if (currentState.gestureNum < TOTAL_GESTURES_NUM) {
                        PreparingForTheNextRecording(
                            directoryUri = currentState.directoryUri,
                            gestureName = currentState.gestureName,
                            gestureNum = currentState.gestureNum.inc(),
                            isOneHandGesture = currentState.isOneHandGesture,
                            isGpuEnabled = currentState.isGpuEnabled,
                            camera = currentState.camera
                        )
                    } else {
                        WaitingForDirectoryAndGesture(
                            directoryUri = currentState.directoryUri,
                            gestureName = currentState.gestureName,
                            isOneHandGesture = currentState.isOneHandGesture,
                            isGpuEnabled = currentState.isGpuEnabled,
                            camera = currentState.camera
                        )
                    }
                }
            }

            is WaitingForTheNextMotionRecording -> if (currentState is PreparingForTheNextRecording) {
                if (event.delayTicks > UInt.MIN_VALUE) {
                    state {
                        currentState.copy(delayTicks = event.delayTicks)
                    }
                } else {
                    state {
                        RecordingMotion(
                            directoryUri = currentState.directoryUri,
                            gestureName = currentState.gestureName,
                            gestureNum = currentState.gestureNum,
                            isOneHandGesture = currentState.isOneHandGesture,
                            isGpuEnabled = currentState.isGpuEnabled,
                            camera = currentState.camera
                        )
                    }
                    commands { +StartRecording }
                }
            }

            is GesturesRate -> if (currentState is WaitingForDirectoryAndGesture) {
                state { currentState.copy(gestureRates = event.rates) }
            }
        }
    }

    override fun Result.ui(event: Ui) {
        val currentState = state
        when (event) {
            is Ui.OnDirectoryChosen -> if (currentState is WaitingForDirectoryAndGesture) {
                val currentGestureName = currentState.gestureName
                state {
                    if (currentGestureName == null) {
                        currentState.copy(directoryUri = event.directory)
                    } else {
                        ReadyToStartRecording(
                            directoryUri = event.directory,
                            gestureName = currentGestureName,
                            isOneHandGesture = currentState.isOneHandGesture,
                            isGpuEnabled = currentState.isGpuEnabled,
                            camera = currentState.camera
                        )
                    }
                }
            }

            is Ui.OnFacePoseResults -> when {
                currentState is RecordingMotion -> {
                    val currentRecording = currentState.facePose

                    val csvRows = currentRecording.rows

                    csvRows.putAll(
                        event.result.poseLandmarks.map { landmark ->
                            landmark.toFrameLandmark(currentRecording.frames)
                        }.associateBy(FrameLandmark::rowId)
                    )
                    event.result.faceLandmarks.map { landmark ->
                        landmark.toFrameLandmark(currentRecording.frames)
                    }.associateBy(FrameLandmark::rowId)
                        .let(csvRows::putAll)

                    state {
                        currentState.copy(
                            facePose = currentRecording.copy(
                                frames = currentRecording.frames.inc(),
                                rows = csvRows
                            )
                        )
                    }
                }

                currentState is WaitingForDirectoryAndGesture -> {
                    val frame: UInt = currentState.facePoseFramesQueue.size
                        .takeIf { size -> size < DETECTION_FRAME_BUFFER_SIZE.toInt() }
                        ?.toUInt()
                        ?: 0u

                    state {
                        currentState.copy(
                            facePoseFramesQueue = currentState.facePoseFramesQueue.apply {
                                add(
                                    buildMap {
                                        putAll(
                                            with(event.result) {
                                                (poseLandmarks + faceLandmarks).asSequence()
                                                    .map { mark -> mark.toFrameLandmark(frame) }
                                                    .map { mark -> mark.rowId to mark }
                                                    .toList()
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }

                    commands {
                        with(currentState) {
                            if (handsFramesQueue.size == DETECTION_FRAME_BUFFER_SIZE.toInt()
                                && facePoseFramesQueue.size == DETECTION_FRAME_BUFFER_SIZE.toInt()
                            ) {
                                +Command.CheckGesture(
                                    DETECTION_FRAME_BUFFER_SIZE,
                                    handsFramesQueue.toList(),
                                    facePoseFramesQueue.toList()
                                )
                            }
                        }
                    }
                }
            }

            is Ui.OnGestureNameChanged -> if (currentState is WaitingForDirectoryAndGesture) {
                val currentDirectory = currentState.directoryUri
                state {
                    if (currentDirectory == null) {
                        currentState.copy(gestureName = event.gestureName)
                    } else {
                        ReadyToStartRecording(
                            currentDirectory,
                            event.gestureName,
                            isOneHandGesture = currentState.isOneHandGesture,
                            isGpuEnabled = currentState.isGpuEnabled,
                            camera = currentState.camera
                        )
                    }
                }
            }

            is Ui.OnHandResults -> when {
                currentState is RecordingMotion -> {
                    val currentRecording = currentState.hands

                    val rows = currentRecording.rows

                    event.handsResults.onEach { handLandmarks ->
                        rows.putAll(
                            handLandmarks.map { mark ->
                                mark.toFrameLandmark(currentRecording.frames)
                            }.associateBy(FrameLandmark::rowId)
                        )
                    }

                    state {
                        currentState.copy(
                            hands = currentRecording.copy(
                                frames = currentRecording.frames.inc(),
                                rows = rows
                            )
                        )
                    }
                }

                currentState is WaitingForDirectoryAndGesture -> {
                    val frame: UInt = currentState.handsFramesQueue.size
                        .takeIf { size -> size < DETECTION_FRAME_BUFFER_SIZE.toInt() }
                        ?.toUInt()
                        ?: 0u

                    state {
                        currentState.copy(
                            facePoseFramesQueue = currentState.handsFramesQueue.apply {
                                add(
                                    buildMap {
                                        putAll(
                                            event.handsResults
                                                .asSequence()
                                                .flatten()
                                                .map { mark -> mark.toFrameLandmark(frame) }
                                                .map { mark -> mark.rowId to mark }
                                                .toList()
                                        )
                                    }
                                )
                            }
                        )
                    }

                    commands {
                        with(currentState) {
                            if (handsFramesQueue.size == DETECTION_FRAME_BUFFER_SIZE.toInt()
                                && facePoseFramesQueue.size == DETECTION_FRAME_BUFFER_SIZE.toInt()
                            ) {
                                +Command.CheckGesture(
                                    DETECTION_FRAME_BUFFER_SIZE,
                                    handsFramesQueue.toList(),
                                    facePoseFramesQueue.toList()
                                )
                            }
                        }
                    }
                }
            }

            is Ui.OnStartRecordingPressed -> if (currentState is ReadyToStartRecording) {
                state {
                    PreparingForTheNextRecording(
                        directoryUri = currentState.directoryUri,
                        gestureName = currentState.gestureName,
                        gestureNum = 1u,
                        isOneHandGesture = currentState.isOneHandGesture,
                        isGpuEnabled = currentState.isGpuEnabled,
                        camera = currentState.camera
                    )
                }
                commands { +PrepareForGestureRecording }
            }

            is Ui.OnResumeRecording -> if (currentState is Pausable) {
                commands { +Command.ResumeRecording }
                state {
                    when (currentState) {
                        is PreparingForTheNextRecording -> currentState.copy(isPaused = false)
                        is RecordingMotion -> currentState.copy(isPaused = false)
                    }
                }
            }

            is Ui.OnPauseRecording -> if (currentState is Pausable) {
                commands { +Command.PauseRecording }
                state {
                    when (currentState) {
                        is PreparingForTheNextRecording -> currentState.copy(isPaused = true)
                        is RecordingMotion -> currentState.copy(isPaused = true)
                    }
                }
            }

            Ui.OnStopRecording -> if (currentState is Pausable) {
                commands { +Command.StopRecording }
                state {
                    WaitingForDirectoryAndGesture(
                        directoryUri = currentState.directoryUri,
                        gestureName = currentState.gestureName,
                        isOneHandGesture = currentState.isOneHandGesture,
                        isGpuEnabled = currentState.isGpuEnabled,
                        camera = currentState.camera
                    )
                }
            }

            Ui.OnToggleCamera -> {
                if (currentState is Steady) {
                    val camerasInfo = currentState.camera
                    if (camerasInfo is AllTypes) {
                        val toggledCamera = camerasInfo.isCurrentFrontFacing.not()
                        when (currentState) {
                            is WaitingForDirectoryAndGesture -> state {
                                currentState.copy(camera = AllTypes(toggledCamera))
                            }

                            is ReadyToStartRecording -> state {
                                currentState.copy(camera = AllTypes(toggledCamera))
                            }
                        }
                    }
                }
            }

            is Ui.OnCamerasInfoReceived -> if (currentState is NoCameraPermitted && event.camerasInfo is CamerasInfo.CamerasAvailable) {
                state {
                    WaitingForDirectoryAndGesture(
                        camera = event.camerasInfo
                    )
                }
            }

            Ui.OnToggleCpuGpu -> if (currentState is LiveCamera) {
                state {
                    currentState.copy(isGpuEnabled = currentState.isGpuEnabled.not())
                }
            }

            Ui.OnToggleHandsCount -> if (currentState is LiveCamera) {
                state {
                    currentState.copy(isOneHandGesture = currentState.isOneHandGesture.not())
                }
            }
        }
    }

    private fun Landmark.toFrameLandmark(frame: UInt): FrameLandmark {
        return FrameLandmark(
            frame = frame,
            landmark = this
        )
    }
}
