package org.landmarkscollector.hands

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.landmarkscollector.data.Landmark.Hand

data class ResultBundle(
    val handLandmarks: List<List<Hand>>,
    val inputImageHeight: Int,
    val inputImageWidth: Int,
) {

    constructor(
        handLandmarkerResult: HandLandmarkerResult,
        inputImageHeight: Int,
        inputImageWidth: Int,
    ) : this(handLandmarkerResult.toHandLandmarks(), inputImageHeight, inputImageWidth)

    companion object {
        private fun HandLandmarkerResult.toHandLandmarks(): List<List<Hand>> {
            val handednesses = handednesses()
            return landmarks().mapIndexed { index, normalizedLandmarks ->
                val handedness = handednesses.getOrNull(index)?.firstOrNull()
                requireNotNull(handedness) { "Both info on landmarks and handedness should be available" }
                normalizedLandmarks.getHandLandmarks(
                    isRightHand = handedness.categoryName() == "Right"
                )
            }
        }

        private fun List<NormalizedLandmark>.getHandLandmarks(
            isRightHand: Boolean,
        ): List<Hand> = mapIndexed { index, normalizedLandmark ->
            normalizedLandmark.toHandLandmark(
                index.toUInt(),
                isRightHand
            )
        }

        private fun NormalizedLandmark.toHandLandmark(
            index: UInt,
            isRightHand: Boolean,
        ): Hand = with(this) {
            if (isRightHand) {
                Hand.Right(index, x(), y(), z())
            } else {
                Hand.Left(index, x(), y(), z())
            }
        }
    }
}
