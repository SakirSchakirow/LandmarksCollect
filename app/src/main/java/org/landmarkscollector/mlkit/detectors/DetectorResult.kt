package org.landmarkscollector.mlkit.detectors

import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.pose.Pose

data class DetectorResult(
    val faces: List<FaceMesh>,
    val pose: Pose
)
