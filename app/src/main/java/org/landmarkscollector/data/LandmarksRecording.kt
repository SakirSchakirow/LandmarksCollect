package org.landmarkscollector.data

data class LandmarksRecording(
    val frames: UInt = UInt.MIN_VALUE,
    val rows: HashMap<RowId, FrameLandmark> = HashMap()
)
