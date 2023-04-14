package org.landmarkscollector.camera

import android.net.Uri
import org.landmarkscollector.data.LandmarksRecording

sealed class Command {

    object PrepareForGestureRecording : Command()

    object StartRecording : Command()

    class SaveRecording(
        val uri: Uri,
        val hands: LandmarksRecording,
        val facePose: LandmarksRecording
    ) : Command()
}
