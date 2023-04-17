package org.landmarkscollector.motionRecording

import android.util.Log
import com.google.mediapipe.tasks.vision.core.RunningMode
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.hands.LandmarkerListener
import org.landmarkscollector.hands.OverlayView
import org.landmarkscollector.hands.ResultBundle
import java.util.concurrent.Executor

class HandsLandmarkerListener(
    private val mainExecutor: Executor,
    private val overlayView: OverlayView,
    private val onHandResults: (handsResults: List<List<Landmark.Hand>>) -> Unit,
) : LandmarkerListener {

    override fun onError(error: String) {
        Log.e("CameraScreen", error)
    }

    override fun onResults(resultBundle: ResultBundle) {
        mainExecutor.execute {
            onHandResults(resultBundle.handLandmarks)
            with(overlayView) {
                setResults(
                    handLandmarks = resultBundle.handLandmarks,
                    imageHeight = resultBundle.inputImageHeight,
                    imageWidth = resultBundle.inputImageWidth,
                    runningMode = RunningMode.LIVE_STREAM
                )
                invalidate()
            }
        }
    }
}
