package org.landmarkscollector.motionRecording

import android.net.Uri
import org.landmarkscollector.data.LandmarksRecording

sealed class Command {

    object PrepareForGestureRecording : Command()

    object StartRecording : Command()
    object PauseRecording : Command()
    object ResumeRecording : Command()
    object StopRecording : Command()

    class SaveRecording(
        val uri: Uri,
        val hands: LandmarksRecording,
        val facePose: LandmarksRecording
    ) : Command()
}
