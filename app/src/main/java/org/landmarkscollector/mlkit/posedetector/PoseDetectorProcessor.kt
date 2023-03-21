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

package org.landmarkscollector.mlkit.posedetector

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.odml.image.MlImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.demo.kotlin.posedetector.PoseGraphic
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.VisionProcessorBase

/** A processor to run pose detector. */
class PoseDetectorProcessor(
    context: Context,
    options: PoseDetectorOptionsBase,
    private val visualizeZ: Boolean,
    private val rescaleZForVisualization: Boolean
) : VisionProcessorBase<Pose>(context) {

    private val detector: PoseDetector

    init {
        detector = PoseDetection.getClient(options)
    }

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage): Task<Pose> {
        return detector.process(image)
    }

    override fun detectInImage(image: MlImage): Task<Pose> {
        return detector.process(image)
    }

    override fun onSuccess(
        results: Pose,
        graphicOverlay: GraphicOverlay
    ) {
        graphicOverlay.add(
            PoseGraphic(
                graphicOverlay,
                results,
                visualizeZ,
                rescaleZForVisualization
            )
        )
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Pose detection failed!", e)
    }

    override fun isMlImageEnabled(context: Context?): Boolean {
        // Use MlImage in Pose Detection by default, change it to OFF to switch to InputImage.
        return false
    }

    companion object {
        private val TAG = "PoseDetectorProcessor"
    }
}
