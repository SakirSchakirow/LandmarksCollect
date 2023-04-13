package org.landmarkscollector.elm

import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.google.mlkit.vision.pose.PoseLandmark
import org.landmarkscollector.data.CsvRow
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.domain.repository.FileCreator
import org.landmarkscollector.elm.Command.SaveRecording
import org.landmarkscollector.elm.Command.StartRecording
import org.landmarkscollector.elm.Event.Ui
import org.landmarkscollector.elm.Event.Internal
import org.landmarkscollector.elm.Event.Internal.RecordIsSaving
import org.landmarkscollector.elm.Event.Internal.RecordingSaved
import org.landmarkscollector.elm.Event.Internal.RecordingTimeLeft
import org.landmarkscollector.elm.Event.Internal.WaitingForTheNextMotionRecording
import org.landmarkscollector.elm.State.Companion.TOTAL_GESTURES_NUM
import org.landmarkscollector.elm.State.Recording.PreparingForTheNextRecording
import org.landmarkscollector.elm.State.Recording.RecordingMotion
import org.landmarkscollector.elm.State.Recording.SavingPreviousMotion
import org.landmarkscollector.elm.State.Steady.WaitingForDirectoryAndGesture
import vivid.money.elmslie.core.store.dsl_reducer.ScreenDslReducer

class Reducer(
    private val fileCreator: FileCreator
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

                val poseLandmarks = event.result.pose.allPoseLandmarks
                    .map { toLandmark(event.imageProxy, it) }
                val faceLandmarks = event.result.faces.firstOrNull()
                    ?.allPoints
                    ?.map { toLandmark(event.imageProxy, it) }

                csvRows.putAll(
                    poseLandmarks.map { landmark ->
                        landmark.toCsvRow(currentRecording.frames)
                    }.associateBy(CsvRow::rowId)
                )
                faceLandmarks?.map { landmark ->
                    landmark.toCsvRow(currentRecording.frames)
                }?.associateBy(CsvRow::rowId)
                    ?.let(csvRows::putAll)

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

                val landmarks = event.results.landmarks()
                val handednesses = event.results.handednesses()
                landmarks.mapIndexed { index, normalizedLandmarks ->
                    val handedness = handednesses.getOrNull(index)?.firstOrNull()
                    requireNotNull(handedness) { "Both info on landmarks and handedness should be available" }
                    val marks = normalizedLandmarks.getLandmarks(
                        isRightHand = handedness.categoryName() == "Right"
                    )

                    csvRows.putAll(
                        marks.map { mark ->
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
                commands { +StartRecording }
                state {
                    RecordingMotion(
                        directoryUri = currentState.directoryUri,
                        gestureName = currentState.gestureName
                    )
                }
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
            Landmark.Pose(
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
            Landmark.Face(
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
                Landmark.Hand.Right(index, x(), y(), z())
            } else {
                Landmark.Hand.Left(index, x(), y(), z())
            }
        }

    private fun Landmark.toCsvRow(frame: UInt): CsvRow {
        return CsvRow(
            frame = frame,
            landmark = this
        )
    }
}
