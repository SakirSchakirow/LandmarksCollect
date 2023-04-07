package org.landmarkscollector.data

typealias RowId = String

data class LandmarksRecording(
    val frames: UInt = UInt.MIN_VALUE,
    val csvRows: HashMap<RowId, CsvRow> = HashMap()
)
