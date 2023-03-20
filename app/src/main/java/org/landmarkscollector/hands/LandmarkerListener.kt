package org.landmarkscollector.hands

interface LandmarkerListener {

    fun onError(error: String)

    fun onResults(resultBundle: ResultBundle)
}
