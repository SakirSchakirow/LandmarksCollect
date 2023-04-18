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

package org.landmarkscollector.mlkit.pose

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.vision.pose.PoseLandmark
import org.landmarkscollector.data.Landmark.Pose
import org.landmarkscollector.mlkit.GraphicOverlay

/** Draw the detected pose in preview. */
class PoseGraphic
internal constructor(
    overlay: GraphicOverlay,
    private val poseLandmarks: List<Pose>,
    private val imageWidth: Int,
    private val imageHeight: Int,
) : GraphicOverlay.Graphic(overlay) {
    private val leftPaint: Paint = Paint()
    private val rightPaint: Paint = Paint()
    private val whitePaint: Paint = Paint()

    init {
        whitePaint.strokeWidth = STROKE_WIDTH
        whitePaint.color = Color.WHITE
        whitePaint.textSize = IN_FRAME_LIKELIHOOD_TEXT_SIZE
        leftPaint.strokeWidth = STROKE_WIDTH
        leftPaint.color = Color.GREEN
        rightPaint.strokeWidth = STROKE_WIDTH
        rightPaint.color = Color.YELLOW
    }

    override fun draw(canvas: Canvas) {
        val landmarks = poseLandmarks
        if (landmarks.isEmpty()) {
            return
        }

        // Draw all the points
        for (landmark in landmarks) {
            drawPoint(canvas, landmark, whitePaint)
        }

        val nose = landmarks[PoseLandmark.NOSE]
        val leftEyeInner = landmarks[PoseLandmark.LEFT_EYE_INNER]
        val leftEye = landmarks[PoseLandmark.LEFT_EYE]
        val leftEyeOuter = landmarks[PoseLandmark.LEFT_EYE_OUTER]
        val rightEyeInner = landmarks[PoseLandmark.RIGHT_EYE_INNER]
        val rightEye = landmarks[PoseLandmark.RIGHT_EYE]
        val rightEyeOuter = landmarks[PoseLandmark.RIGHT_EYE_OUTER]
        val leftEar = landmarks[PoseLandmark.LEFT_EAR]
        val rightEar = landmarks[PoseLandmark.RIGHT_EAR]
        val leftMouth = landmarks[PoseLandmark.LEFT_MOUTH]
        val rightMouth = landmarks[PoseLandmark.RIGHT_MOUTH]

        val leftShoulder = landmarks[PoseLandmark.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmark.RIGHT_SHOULDER]
        val leftElbow = landmarks[PoseLandmark.LEFT_ELBOW]
        val rightElbow = landmarks[PoseLandmark.RIGHT_ELBOW]
        val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        val rightHip = landmarks[PoseLandmark.RIGHT_HIP]
        val leftKnee = landmarks[PoseLandmark.LEFT_KNEE]
        val rightKnee = landmarks[PoseLandmark.RIGHT_KNEE]
        val leftAnkle = landmarks[PoseLandmark.LEFT_ANKLE]
        val rightAnkle = landmarks[PoseLandmark.RIGHT_ANKLE]

        val leftPinky = landmarks[PoseLandmark.LEFT_PINKY]
        val rightPinky = landmarks[PoseLandmark.RIGHT_PINKY]
        val leftIndex = landmarks[PoseLandmark.LEFT_INDEX]
        val rightIndex = landmarks[PoseLandmark.RIGHT_INDEX]
        val leftThumb = landmarks[PoseLandmark.LEFT_THUMB]
        val rightThumb = landmarks[PoseLandmark.RIGHT_THUMB]
        val leftHeel = landmarks[PoseLandmark.LEFT_HEEL]
        val rightHeel = landmarks[PoseLandmark.RIGHT_HEEL]
        val leftFootIndex = landmarks[PoseLandmark.LEFT_FOOT_INDEX]
        val rightFootIndex = landmarks[PoseLandmark.RIGHT_FOOT_INDEX]

        // Face
        drawLine(canvas, nose, leftEyeInner, whitePaint)
        drawLine(canvas, leftEyeInner, leftEye, whitePaint)
        drawLine(canvas, leftEye, leftEyeOuter, whitePaint)
        drawLine(canvas, leftEyeOuter, leftEar, whitePaint)
        drawLine(canvas, nose, rightEyeInner, whitePaint)
        drawLine(canvas, rightEyeInner, rightEye, whitePaint)
        drawLine(canvas, rightEye, rightEyeOuter, whitePaint)
        drawLine(canvas, rightEyeOuter, rightEar, whitePaint)
        drawLine(canvas, leftMouth, rightMouth, whitePaint)

        drawLine(canvas, leftShoulder, rightShoulder, whitePaint)
        drawLine(canvas, leftHip, rightHip, whitePaint)

        // Left body
        drawLine(canvas, leftShoulder, leftElbow, leftPaint)
        drawLine(canvas, leftElbow, leftWrist, leftPaint)
        drawLine(canvas, leftShoulder, leftHip, leftPaint)
        drawLine(canvas, leftHip, leftKnee, leftPaint)
        drawLine(canvas, leftKnee, leftAnkle, leftPaint)
        drawLine(canvas, leftWrist, leftThumb, leftPaint)
        drawLine(canvas, leftWrist, leftPinky, leftPaint)
        drawLine(canvas, leftWrist, leftIndex, leftPaint)
        drawLine(canvas, leftIndex, leftPinky, leftPaint)
        drawLine(canvas, leftAnkle, leftHeel, leftPaint)
        drawLine(canvas, leftHeel, leftFootIndex, leftPaint)

        // Right body
        drawLine(canvas, rightShoulder, rightElbow, rightPaint)
        drawLine(canvas, rightElbow, rightWrist, rightPaint)
        drawLine(canvas, rightShoulder, rightHip, rightPaint)
        drawLine(canvas, rightHip, rightKnee, rightPaint)
        drawLine(canvas, rightKnee, rightAnkle, rightPaint)
        drawLine(canvas, rightWrist, rightThumb, rightPaint)
        drawLine(canvas, rightWrist, rightPinky, rightPaint)
        drawLine(canvas, rightWrist, rightIndex, rightPaint)
        drawLine(canvas, rightIndex, rightPinky, rightPaint)
        drawLine(canvas, rightAnkle, rightHeel, rightPaint)
        drawLine(canvas, rightHeel, rightFootIndex, rightPaint)
    }

    private fun drawPoint(canvas: Canvas, landmark: Pose, paint: Paint) {
        canvas.drawCircle(
            translateX(landmark.x * imageWidth),
            translateY(landmark.y * imageHeight),
            DOT_RADIUS,
            paint
        )
    }

    private fun drawLine(
        canvas: Canvas,
        startLandmark: Pose,
        endLandmark: Pose,
        paint: Paint,
    ) {

        canvas.drawLine(
            translateX(startLandmark.x * imageWidth),
            translateY(startLandmark.y * imageHeight),
            translateX(endLandmark.x * imageWidth),
            translateY(endLandmark.y * imageHeight),
            paint
        )
    }

    companion object {

        private const val DOT_RADIUS = 8.0f
        private const val IN_FRAME_LIKELIHOOD_TEXT_SIZE = 30.0f
        private const val STROKE_WIDTH = 10.0f
    }
}
