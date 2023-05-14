package org.landmarkscollector.motionRecording

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.landmarkscollector.data.FrameLandmark
import org.landmarkscollector.data.FrameLandmark.Companion.rowId
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.data.Landmark.LandmarkType.Face
import org.landmarkscollector.data.Landmark.LandmarkType.LeftHand
import org.landmarkscollector.data.Landmark.LandmarkType.Pose
import org.landmarkscollector.data.Landmark.LandmarkType.RightHand
import org.landmarkscollector.data.Resource
import org.landmarkscollector.data.RowId
import org.landmarkscollector.domain.repository.ExportRepository
import org.landmarkscollector.motionRecording.Event.Internal.GesturesRate
import org.landmarkscollector.motionRecording.Event.Internal.RecordingTimeLeft
import org.landmarkscollector.motionRecording.Event.Internal.WaitingForTheNextMotionRecording
import org.landmarkscollector.motionRecording.State.Companion.DELAY_SECS
import org.landmarkscollector.motionRecording.State.Companion.MAX_FRAMES_SECS
import org.tensorflow.lite.Interpreter
import vivid.money.elmslie.core.store.DefaultActor
import kotlin.time.Duration.Companion.seconds

internal class Actor(
    private val interpreter: () -> Interpreter,
    private val exportRepository: ExportRepository,
) : DefaultActor<Command, Event> {

    private val recordingSemaphore: Semaphore = Semaphore(1)

    override fun execute(command: Command): Flow<Event> = when (command) {
        is Command.CheckGesture -> flow {
            val tensor = getInputTensor(
                command.bufferSize,
                command.facePoseFramesQueue,
                command.handsFramesQueue
            )
            val output: Array<FloatArray> = arrayOf(
                floatArrayOf(
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f
                )
            )

            val interpreter = interpreter()

            interpreter.run(arrayOf(tensor), output)

            val result = output[0]

            val labels = listOf(
                "день",
                "здравствуйте",
                "нормально",
                "спасибо",
                "я",
                "дела",
                "добрый",
                "пожалуйста",
                "хорошо",
                "до свидания",
                "добрый день",
            )
            emit(
                GesturesRate(
                    buildMap {
                        result.mapIndexed { index, prob ->
                            put(labels[index], prob)
                        }
                    }
                )
            )
        }

        is Command.SaveRecording -> flow {
            exportRepository.startExportData(
                exportList = getFulfilledFramesCsvRows(
                    facesPoses = command.facePose.rows,
                    facesPosesNumber = command.facePose.frames,
                    hands = command.hands.rows,
                    handsNumber = command.hands.frames
                ),
                futureCsvFile = command.uri
            ).collect { resource ->
                when (resource) {
                    is Resource.Error -> TODO()
                    is Resource.Loading -> emit(
                        Event.Internal.RecordIsSaving(
                            resource.data?.progressPercentage?.toUInt() ?: 0u
                        )
                    )

                    is Resource.Success -> {
                        emit(Event.Internal.RecordingSaved)
                        for (delayTicks in DELAY_SECS downTo UInt.MIN_VALUE) {
                            delay(1.seconds)
                            emit(WaitingForTheNextMotionRecording(delayTicks))
                        }
                    }
                }
            }
        }

        Command.PrepareForGestureRecording -> flow {
            for (delayTicks in DELAY_SECS downTo UInt.MIN_VALUE) {
                recordingSemaphore.withPermit {
                    delay(1.seconds)
                    emit(WaitingForTheNextMotionRecording(delayTicks))
                }
            }
        }

        Command.StartRecording -> flow {
            for (timeLeft in MAX_FRAMES_SECS downTo UInt.MIN_VALUE) {
                recordingSemaphore.withPermit {
                    delay(1.seconds)
                    emit(RecordingTimeLeft(timeLeft))
                }
            }
        }

        Command.PauseRecording -> flow {
            recordingSemaphore.acquire()
        }

        Command.ResumeRecording -> flow {
            recordingSemaphore.release()
        }

        Command.StopRecording -> flow {
            recordingSemaphore.release()
        }
    }

    private fun getFulfilledFramesCsvRows(
        facesPoses: Map<String, FrameLandmark>,
        facesPosesNumber: UInt,
        hands: Map<String, FrameLandmark>,
        handsNumber: UInt,
    ): List<FrameLandmark> {
        val totalFramesNumber = Integer.min(facesPosesNumber.toInt(), handsNumber.toInt()).toUInt()
        return buildList {
            for (frameNumber in UInt.MIN_VALUE until totalFramesNumber) {
                addLandmarks(frameNumber, facesPoses, Face)
                addLandmarks(frameNumber, hands, RightHand)
                addLandmarks(frameNumber, hands, LeftHand)
                addLandmarks(frameNumber, facesPoses, Pose)
            }
        }
    }

    private fun MutableList<FrameLandmark>.addLandmarks(
        frameNumber: UInt,
        rows: Map<String, FrameLandmark>,
        landmarkType: Landmark.LandmarkType,
    ) {
        for (landmarkIndex in UInt.MIN_VALUE until landmarkType.totalLandmarkNumber) {
            val rowId = landmarkType.rowId(frameNumber, landmarkIndex)
            val frameLandmark =
                rows[rowId] ?: FrameLandmark.empty(frameNumber, landmarkIndex, landmarkType)
            add(frameLandmark)
        }
    }

    private fun getInputTensor(
        bufferSize: UInt,
        hands: List<Map<RowId, FrameLandmark>>,
        facePoses: List<Map<RowId, FrameLandmark>>,
    ): Array<FloatArray> {
        return buildList {
            for (frameNumber in UInt.MIN_VALUE until bufferSize) {
                add(
                    buildList {
                        addFloats(frameNumber, facePoses[frameNumber.toInt()], Face)
                        addFloats(frameNumber, hands[frameNumber.toInt()], RightHand)
                        addFloats(frameNumber, hands[frameNumber.toInt()], LeftHand)
                        addFloats(frameNumber, facePoses[frameNumber.toInt()], Pose)
                    }.toFloatArray()
                )
            }
        }.toTypedArray()
    }

    private fun MutableList<Float>.addFloats(
        frameNumber: UInt,
        rows: Map<RowId, FrameLandmark>,
        landmarkType: Landmark.LandmarkType,
    ) {
        for (landmarkIndex in UInt.MIN_VALUE until landmarkType.totalLandmarkNumber) {
            val rowId = landmarkType.rowId(frameNumber, landmarkIndex)
            val frameLandmark =
                rows[rowId] ?: FrameLandmark.empty(frameNumber, landmarkIndex, landmarkType)
            add(frameLandmark.x ?: 0f)
            add(frameLandmark.y ?: 0f)
        }
    }
}
