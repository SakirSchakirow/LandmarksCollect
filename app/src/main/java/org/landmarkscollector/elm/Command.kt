package org.landmarkscollector.elm

import android.net.Uri
import org.landmarkscollector.data.LandmarksRecording

sealed class Command {

    object StartRecording : Command()

    class SaveRecording(
        val uri: Uri,
        val hands: LandmarksRecording,
        val facePose: LandmarksRecording
    ) : Command()
}
