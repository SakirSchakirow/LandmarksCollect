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

package org.landmarkscollector.mlkit.face

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.landmarkscollector.data.Landmark.Face
import org.landmarkscollector.mlkit.GraphicOverlay

/**
 * Graphic instance for rendering face position and mesh info within the associated graphic overlay
 * view.
 */
class FaceMeshGraphic(
    overlay: GraphicOverlay,
    private val points: List<Face>,
    private val imageWidth: Int,
    private val imageHeight: Int,
) :
    GraphicOverlay.Graphic(overlay) {

    private val positionPaint: Paint
    private val boxPaint: Paint

    /** Draws the face annotations for position on the supplied canvas. */
    override fun draw(canvas: Canvas) {
        // Draw face mesh points
        for (point in points) {
            canvas.drawCircle(
                translateX(point.x * imageWidth),
                translateY(point.y * imageHeight),
                FACE_POSITION_RADIUS,
                positionPaint
            )
        }
    }

    companion object {
        private const val FACE_POSITION_RADIUS = 8.0f
        private const val BOX_STROKE_WIDTH = 5.0f
    }

    init {
        val selectedColor = Color.WHITE
        positionPaint = Paint()
        positionPaint.color = selectedColor

        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
    }
}
