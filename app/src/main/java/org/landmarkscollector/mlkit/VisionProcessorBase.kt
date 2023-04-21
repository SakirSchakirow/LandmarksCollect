/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.landmarkscollector.mlkit

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.GuardedBy
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.android.gms.tasks.Tasks
import com.google.android.odml.image.MlImage
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import java.lang.Math.max
import java.lang.Math.min
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask

@ExperimentalGetImage
/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
abstract class VisionProcessorBase<T>(context: Context) : VisionImageProcessor<T> {

    companion object {
        private const val TAG = "VisionProcessorBase"
    }

    private var activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    // Whether this processor is already shut down
    private var isShutdown = false

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null

    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null

    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    @Synchronized
    private fun processLatestImage(
        graphicOverlay: GraphicOverlay?,
        onSuccess: ((T) -> Unit)?
    ) {
        processingImage = latestImage
        processingMetaData = latestImageMetaData
        latestImage = null
        latestImageMetaData = null
        if (processingImage != null && processingMetaData != null && !isShutdown) {
            processImage(processingImage!!, processingMetaData!!, graphicOverlay, onSuccess)
        }
    }

    private fun processImage(
        data: ByteBuffer,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay?,
        onSuccess: ((T) -> Unit)?
    ) {
        requestDetectInImage(
            InputImage.fromByteBuffer(
                data,
                frameMetadata.width,
                frameMetadata.height,
                frameMetadata.rotation,
                InputImage.IMAGE_FORMAT_NV21
            ),
            graphicOverlay,
            onSuccess
        )
            .addOnSuccessListener(executor) { processLatestImage(graphicOverlay, onSuccess) }
    }

    // -----------------Code for processing live preview frame from CameraX API-----------------------
    override fun  processImageProxy(
        image: ImageProxy,
        graphicOverlay: GraphicOverlay?,
        onSuccess: ((T) -> Unit)?
    ) {
        if (isShutdown) {
            return
        }

        requestDetectInImage(
            InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees),
            graphicOverlay,
            onSuccess
        )

            // When the image is from CameraX analysis use case, must call image.close() on received
            // images when finished using them. Otherwise, new images may not be received or the camera
            // may stall.
            .addOnCompleteListener { image.close() }
    }

    // -----------------Common processing logic-------------------------------------------------------
    private fun requestDetectInImage(
        image: InputImage,
        graphicOverlay: GraphicOverlay?,
        onSuccess: ((T) -> Unit)?
    ): Task<T> {
        return setUpListener(
            detectInImage(image),
            graphicOverlay,
            onSuccess
        )
    }

    private fun setUpListener(
        task: Task<T>,
        graphicOverlay: GraphicOverlay?,
        onSuccess: ((T) -> Unit)?
    ): Task<T> {
        return task
            .addOnSuccessListener(
                executor
            ) { results: T ->
                onSuccess?.invoke(results)
                graphicOverlay?.clear()
                this@VisionProcessorBase.onSuccess(results, graphicOverlay)
                graphicOverlay?.postInvalidate()
            }
            .addOnFailureListener(
                executor
            ) { e: Exception ->
                graphicOverlay?.clear()
                graphicOverlay?.postInvalidate()
                Log.d(TAG, "Failed to process. Error: " + e.localizedMessage)
                e.printStackTrace()
                this@VisionProcessorBase.onFailure(e)
            }
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected abstract fun onSuccess(
        results: T,
        graphicOverlay: GraphicOverlay?
    ): T

    protected abstract fun onFailure(e: Exception)
}
