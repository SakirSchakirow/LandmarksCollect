package org.landmarkscollector.motionRecording

import android.net.Uri
import com.google.common.collect.EvictingQueue
import org.landmarkscollector.data.FrameLandmark
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.data.LandmarksRecording
import org.landmarkscollector.data.RowId

sealed class Command {

    class CheckGesture(
        val bufferSize: UInt,
        val handsFramesQueue: List<Map<RowId, FrameLandmark>>,
        val facePoseFramesQueue: List<Map<RowId, FrameLandmark>>,
    ) : Command()

    object PrepareForGestureRecording : Command()

    object StartRecording : Command()
    object PauseRecording : Command()
    object ResumeRecording : Command()
    object StopRecording : Command()

    class SaveRecording(
        val uri: Uri,
        val hands: LandmarksRecording,
        val facePose: LandmarksRecording,
    ) : Command()
}
