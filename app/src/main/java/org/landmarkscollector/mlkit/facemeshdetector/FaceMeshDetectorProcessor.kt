/*
 * Copyright 2022 Google LLC. All rights reserved.
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

package org.landmarkscollector.mlkit.facemeshdetector

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import org.landmarkscollector.mlkit.GraphicOverlay
import org.landmarkscollector.mlkit.VisionProcessorBase

/** Face Mesh Detector Demo. */
class FaceMeshDetectorProcessor(context: Context) :
    VisionProcessorBase<List<FaceMesh>>(context) {

    private val detector: FaceMeshDetector

    init {
        val optionsBuilder = FaceMeshDetectorOptions.Builder()
        detector = FaceMeshDetection.getClient(optionsBuilder.build())
    }

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage): Task<List<FaceMesh>> {
        return detector.process(image)
    }

    override fun onSuccess(results: List<FaceMesh>, graphicOverlay: GraphicOverlay) {
        for (face in results) {
            graphicOverlay.add(FaceMeshGraphic(graphicOverlay, face))
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Face detection failed $e")
    }

    companion object {
        private const val TAG = "SelfieFaceProcessor"
    }
}
