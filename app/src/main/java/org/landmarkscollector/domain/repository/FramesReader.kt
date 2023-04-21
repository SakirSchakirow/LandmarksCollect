package org.landmarkscollector.domain.repository

import org.landmarkscollector.data.Frame
import java.io.InputStream

interface FramesReader {

    fun readFrames(inputStream: InputStream): List<Frame>
}
