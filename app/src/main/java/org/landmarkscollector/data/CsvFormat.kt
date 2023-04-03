package org.landmarkscollector.data

data class CsvRow(
    val frame: Int,
    val landmarkIndex: Int,
    val rowId: String,
    val type: String,
    val x: Float?,
    val y: Float?,
    val z: Float?
)
