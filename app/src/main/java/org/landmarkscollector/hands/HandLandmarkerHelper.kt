package org.landmarkscollector.hands

import android.content.Context
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.landmarkscollector.hands.BitmapUtils.getBitmap

private const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"

class HandLandmarkerHelper(
    context: Context,
    isOneHand: Boolean,
    isGpu: Boolean,
    private val landmarkerListener: LandmarkerListener,
) {

    private var handLandmarker: HandLandmarker? = null

    init {
        val baseOptionBuilder = BaseOptions.builder()
            .setDelegate(if (isGpu) Delegate.GPU else Delegate.CPU)
            .setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        try {
            val baseOptions = baseOptionBuilder.build()
            // Create an option builder with base options and specific
            // options only use for Hand Landmarker.
            val optionsBuilder =
                HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinHandDetectionConfidence(0.5F)
                    .setMinTrackingConfidence(0.5F)
                    .setMinHandPresenceConfidence(0.5F)
                    .setNumHands(if (isOneHand) 1 else 2)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(landmarkerListener::onError)

            val options = optionsBuilder.build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            landmarkerListener.onError(e)
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            landmarkerListener.onError(e)
        }
    }

    // Convert the ImageProxy to MP Image and feed it to HandlandmakerHelper.
    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean,
    ) {
        val frameTime = SystemClock.uptimeMillis()

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(
            getBitmap(imageProxy, isFrontCamera)
        ).build()

        detectAsync(mpImage, frameTime)
    }

    // Run hand hand landmark using MediaPipe Hand Landmarker API
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        handLandmarker?.detectAsync(mpImage, frameTime)
        // As we're using running mode LIVE_STREAM, the landmark result will
        // be returned in returnLivestreamResult function
    }

    // Return the landmark result to this HandLandmarkerHelper's caller
    private fun returnLivestreamResult(
        result: HandLandmarkerResult,
        input: MPImage,
    ) {
        landmarkerListener.onResults(
            ResultBundle(
                result,
                input.height,
                input.width
            )
        )
    }
}
