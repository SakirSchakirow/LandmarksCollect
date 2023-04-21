package org.landmarkscollector.motionRecording

import org.landmarkscollector.data.CsvRow
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.domain.repository.FileCreator
import org.landmarkscollector.motionRecording.Command.PrepareForGestureRecording
import org.landmarkscollector.motionRecording.Command.SaveRecording
import org.landmarkscollector.motionRecording.Command.StartRecording
import org.landmarkscollector.motionRecording.Event.Ui
import org.landmarkscollector.motionRecording.Event.Internal
import org.landmarkscollector.motionRecording.Event.Internal.RecordIsSaving
import org.landmarkscollector.motionRecording.Event.Internal.RecordingSaved
import org.landmarkscollector.motionRecording.Event.Internal.RecordingTimeLeft
import org.landmarkscollector.motionRecording.Event.Internal.WaitingForTheNextMotionRecording
import org.landmarkscollector.motionRecording.State.Companion.TOTAL_GESTURES_NUM
import org.landmarkscollector.motionRecording.State.Recording.Pausable
import org.landmarkscollector.motionRecording.State.Recording.Pausable.PreparingForTheNextRecording
import org.landmarkscollector.motionRecording.State.Recording.Pausable.RecordingMotion
import org.landmarkscollector.motionRecording.State.Recording.SavingPreviousMotion
import org.landmarkscollector.motionRecording.State.Steady.WaitingForDirectoryAndGesture
import vivid.money.elmslie.core.store.dsl_reducer.ScreenDslReducer

class Reducer(
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
                            gestureNum = currentState.gestureNum
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
                            gestureNum = currentState.gestureNum.inc()
                        )
                    } else {
                        WaitingForDirectoryAndGesture(
                            directoryUri = currentState.directoryUri,
                            gestureName = currentState.gestureName
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
                            gestureNum = currentState.gestureNum
                        )
                    }
                    commands { +StartRecording }
                }
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

                        State.Steady.ReadyToStartRecording(
                            event.directory,
                            currentGestureName
                        )
                    }
                }
            }

            is Ui.OnFacePoseResults -> if (currentState is RecordingMotion) {

                val currentRecording = currentState.facePose

                val csvRows = currentRecording.csvRows

                csvRows.putAll(
                    event.result.poseLandmarks.map { landmark ->
                        landmark.toCsvRow(currentRecording.frames)
                    }.associateBy(CsvRow::rowId)
                )
                event.result.faceLandmarks.map { landmark ->
                    landmark.toCsvRow(currentRecording.frames)
                }.associateBy(CsvRow::rowId)
                    .let(csvRows::putAll)

                state {
                    currentState.copy(
                        facePose = currentRecording.copy(
                            frames = currentRecording.frames.inc(),
                            csvRows = csvRows
                        )
                    )
                }
            }

            is Ui.OnGestureNameChanged -> if (currentState is WaitingForDirectoryAndGesture) {
                val currentDirectory = currentState.directoryUri
                state {
                    if (currentDirectory == null) {
                        currentState.copy(gestureName = event.gestureName)
                    } else {
                        State.Steady.ReadyToStartRecording(
                            currentDirectory,
                            event.gestureName
                        )
                    }
                }
            }

            is Ui.OnHandResults -> if (currentState is RecordingMotion) {
                val currentRecording = currentState.hands

                val csvRows = currentRecording.csvRows

                event.handsResults.onEach { handLandmarks ->
                    csvRows.putAll(
                        handLandmarks.map { mark ->
                            mark.toCsvRow(currentRecording.frames)
                        }.associateBy(CsvRow::rowId)
                    )
                }

                state {
                    currentState.copy(
                        hands = currentRecording.copy(
                            frames = currentRecording.frames.inc(),
                            csvRows = csvRows
                        )
                    )
                }
            }

            is Ui.OnStartRecordingPressed -> if (currentState is State.Steady.ReadyToStartRecording) {
                state {
                    PreparingForTheNextRecording(
                        directoryUri = currentState.directoryUri,
                        gestureName = currentState.gestureName,
                        gestureNum = 1u
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
                        gestureName = currentState.gestureName
                    )
                }
            }
        }
    }

    private fun Landmark.toCsvRow(frame: UInt): CsvRow {
        return CsvRow(
            frame = frame,
            landmark = this
        )
    }
}
