package org.landmarkscollector.data

data class CsvRow(
    val frame: Int,
    val landmarkIndex: Int,
    val type: Landmark.LandmarkType,
    val x: Float?,
    val y: Float?,
    val z: Float?
) {
    val rowId: String = type.getRowId(frame, landmarkIndex)

    companion object {

        fun Landmark.LandmarkType.getRowId(
            frameNumber: Int,
            landmarkIndex: Int,
        ): String = "$frameNumber-$typeName-$landmarkIndex"
    }
}
