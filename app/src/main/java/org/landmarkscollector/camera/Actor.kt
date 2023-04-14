package org.landmarkscollector.camera

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.landmarkscollector.data.CsvRow
import org.landmarkscollector.data.CsvRow.Companion.rowId
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.data.Resource
import org.landmarkscollector.domain.repository.ExportRepository
import org.landmarkscollector.camera.Event.Internal.RecordingTimeLeft
import org.landmarkscollector.camera.Event.Internal.WaitingForTheNextMotionRecording
import org.landmarkscollector.camera.State.Companion.DELAY_SECS
import org.landmarkscollector.camera.State.Companion.MAX_FRAMES_SECS
import vivid.money.elmslie.core.store.DefaultActor
import kotlin.time.Duration.Companion.seconds

class Actor(
    private val exportRepository: ExportRepository
) : DefaultActor<Command, Event> {

    private val recordingSemaphore: Semaphore = Semaphore(1)

    override fun execute(command: Command): Flow<Event> = when (command) {
        is Command.SaveRecording -> flow {
            exportRepository.startExportData(
                exportList = getFulfilledFramesCsvRows(
                    facesPoses = command.facePose.csvRows,
                    facesPosesNumber = command.facePose.frames,
                    hands = command.hands.csvRows,
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
        facesPoses: Map<String, CsvRow>,
        facesPosesNumber: UInt,
        hands: Map<String, CsvRow>,
        handsNumber: UInt,
    ): List<CsvRow> {
        val totalFramesNumber = Integer.min(facesPosesNumber.toInt(), handsNumber.toInt()).toUInt()
        return buildList {
            for (frameNumber in UInt.MIN_VALUE until totalFramesNumber) {
                addLandmarks(frameNumber, facesPoses, Landmark.LandmarkType.Face)
                addLandmarks(frameNumber, hands, Landmark.LandmarkType.RightHand)
                addLandmarks(frameNumber, hands, Landmark.LandmarkType.LeftHand)
                addLandmarks(frameNumber, facesPoses, Landmark.LandmarkType.Pose)
            }
        }
    }

    private fun MutableList<CsvRow>.addLandmarks(
        frameNumber: UInt,
        rows: Map<String, CsvRow>,
        landmarkType: Landmark.LandmarkType
    ) {
        for (landmarkIndex in UInt.MIN_VALUE until landmarkType.totalLandmarkNumber) {
            val rowId = landmarkType.rowId(frameNumber, landmarkIndex)
            val csvRow = rows[rowId] ?: CsvRow.empty(frameNumber, landmarkIndex, landmarkType)
            add(csvRow)
        }
    }
}
