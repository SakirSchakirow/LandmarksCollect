package org.landmarkscollector.data

import org.landmarkscollector.data.Landmark.LandmarkType

data class FrameLandmark(
    val frame: UInt,
    val landmarkIndex: UInt,
    val rowId: String,
    val type: String,
    val x: Float?,
    val y: Float?,
    val z: Float?
) {

    constructor(
        frame: UInt,
        landmark: Landmark
    ) : this(
        frame = frame,
        landmarkIndex = landmark.landmarkIndex,
        rowId = landmark.rowId(frame),
        type = landmark.type.label,
        x = landmark.x,
        y = landmark.y,
        z = landmark.z
    )

    companion object {

        fun empty(
            frameNumber: UInt,
            landmarkIndex: UInt,
            landmarkType: LandmarkType
        ) = FrameLandmark(
            frame = frameNumber,
            landmarkIndex = landmarkIndex,
            rowId = landmarkType.rowId(frameNumber, landmarkIndex),
            type = landmarkType.label,
            x = null,
            y = null,
            z = null
        )

        fun LandmarkType.rowId(
            frameNumber: UInt,
            landmarkIndex: UInt,
        ): String = "$frameNumber-$label-$landmarkIndex"

        private fun Landmark.rowId(
            frameNumber: UInt
        ): String = "$frameNumber-${type.label}-$landmarkIndex"
    }
}
