package org.landmarkscollector.hands

import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

data class ResultBundle(
    val result: HandLandmarkerResult,
    val inputImageHeight: Int,
    val inputImageWidth: Int,
)
