package org.landmarkscollector.mlkit.detectors

import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.google.mlkit.vision.pose.PoseLandmark
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.data.Landmark.Face

data class DetectorResult(
    val imageWidth: Int,
    val imageHeight: Int,
    val faceLandmarks: List<Face>,
    val poseLandmarks: List<Landmark.Pose>,
) {
    constructor(
        imageWidth: Int,
        imageHeight: Int,
        faceMeshes: FaceMesh,
        poseLandmarks: List<PoseLandmark>,
    ) : this(
        imageWidth,
        imageHeight,
        faceMeshes.toLandmarks(imageWidth, imageHeight),
        poseLandmarks.toLandmark(imageWidth, imageHeight)
    )

    companion object {

        private fun FaceMesh.toLandmarks(imageWidth: Int, imageHeight: Int): List<Face> {
            return allPoints.map { point ->
                point.toLandmark(imageWidth, imageHeight)
            }
        }

        private fun FaceMeshPoint.toLandmark(imageWidth: Int, imageHeight: Int): Face {
            return with(position) {
                val normalizedX = x / imageWidth
                val normalizedY = y / imageHeight
                //the unit of measure for the z is the same as x and Y
                // thus, we consider normalized-z preserve the same ratio
                val normalizedZ = normalizedX * x / z
                Face(
                    landmarkIndex = index.toUInt(),
                    x = normalizedX,
                    y = normalizedY,
                    z = normalizedZ
                )
            }
        }

        private fun List<PoseLandmark>.toLandmark(
            imageWidth: Int,
            imageHeight: Int,
        ): List<Landmark.Pose> {
            return map { it.toLandmark(imageWidth, imageHeight) }
        }

        private fun PoseLandmark.toLandmark(
            imageWidth: Int,
            imageHeight: Int,
        ): Landmark.Pose {
            return with(position3D) {
                val normalizedX = x / imageWidth
                val normalizedY = y / imageHeight
                //the magnitude of z is roughly the same as x
                // thus, we consider normalized-z preserve the same ratio
                val normalizedZ = normalizedX * x / z
                Landmark.Pose(
                    landmarkIndex = landmarkType.toUInt(),
                    x = normalizedX,
                    y = normalizedY,
                    z = normalizedZ
                )
            }
        }
    }
}
