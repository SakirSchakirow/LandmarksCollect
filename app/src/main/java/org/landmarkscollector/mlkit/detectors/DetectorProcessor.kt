package org.landmarkscollector.mlkit.detectors

import android.content.Context
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.demo.kotlin.posedetector.PoseGraphic
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.VisionProcessorBase
import org.landmarkscollector.mlkit.face.FaceMeshGraphic
import java.util.concurrent.Executors

@ExperimentalGetImage
class DetectorProcessor(context: Context) : VisionProcessorBase<DetectorResult>(context) {

    companion object {
        private val TAG = "PoseDetectorProcessor"
    }

    private val faceMeshDetector: FaceMeshDetector = FaceMeshDetection.getClient(
        FaceMeshDetectorOptions.Builder()
            .build()
    )
    private val poseDetector: PoseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()//PoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)//PoseDetectorOptions.STREAM_MODE)
            .setPreferredHardwareConfigs(AccuratePoseDetectorOptions.CPU_GPU)//PoseDetectorOptions.CPU_GPU)
            .build()
    )
    private val classificationExecutor = Executors.newSingleThreadExecutor()

    override fun detectInImage(image: InputImage): Task<DetectorResult> {
        Log.d("DetectorProcessor", "Vision Image Width:${image.width}")
        Log.d("DetectorProcessor", "Vision Image Height:${image.height}")
        return faceMeshDetector.process(image)
            .continueWithTask { facesTask ->
                poseDetector.process(image)
                    .continueWith(classificationExecutor) { poseTask ->
                        DetectorResult(facesTask.result, poseTask.result)
                    }
            }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Pose detection failed!", e)
    }

    override fun onSuccess(
        results: DetectorResult,
        graphicOverlay: GraphicOverlay?
    ): DetectorResult {
        graphicOverlay?.drawResults(results)
        return results
    }

    private fun GraphicOverlay.drawResults(results: DetectorResult) {
        add(
            PoseGraphic(
                overlay = this,
                pose = results.pose,
                visualizeZ = true,
                rescaleZForVisualization = true
            )
        )
        results.faces.forEach { face ->
            add(FaceMeshGraphic(overlay = this, faceMesh = face))
        }
    }
}
