package org.landmarkscollector.data

data class CsvRow(
    val frame: Int,
    val landmarkIndex: Int,
    val type: String,
    val x: Float?,
    val y: Float?,
    val z: Float?
) {
    val rowId = "${frame}_${type}_${landmarkIndex}"
}
