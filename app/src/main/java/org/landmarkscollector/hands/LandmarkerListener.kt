package org.landmarkscollector.hands

interface LandmarkerListener {

    fun onError(error: Exception)

    fun onResults(resultBundle: ResultBundle)
}
