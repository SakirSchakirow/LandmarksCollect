package org.landmarkscollector.data

import org.landmarkscollector.data.Landmark.LandmarkType

data class CsvRow(
    val frame: Int,
    val landmarkIndex: Int,
    val rowId: String,
    val type: String,
    val x: Float?,
    val y: Float?,
    val z: Float?
) {

    constructor(
        frame: Int,
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
            frameNumber: Int,
            landmarkIndex: Int,
            landmarkType: LandmarkType
        ) = CsvRow(
            frame = frameNumber,
            landmarkIndex = landmarkIndex,
            rowId = landmarkType.rowId(frameNumber, landmarkIndex),
            type = landmarkType.label,
            x = null,
            y = null,
            z = null
        )

        fun LandmarkType.rowId(
            frameNumber: Int,
            landmarkIndex: Int,
        ): String = "$frameNumber-$label-$landmarkIndex"

        private fun Landmark.rowId(
            frameNumber: Int
        ): String = "$frameNumber-${type.label}-$landmarkIndex"
    }
}
