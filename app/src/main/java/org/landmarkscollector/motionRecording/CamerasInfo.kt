package org.landmarkscollector.motionRecording

internal sealed interface CamerasInfo {

    val isCurrentFrontFacing: Boolean?

    sealed interface CamerasAvailable : CamerasInfo {

        override val isCurrentFrontFacing: Boolean

        class AllTypes(override val isCurrentFrontFacing: Boolean = true) : CamerasAvailable

        object OnlyFacing : CamerasAvailable {

            override val isCurrentFrontFacing: Boolean = true
        }

        object OnlyBack : CamerasAvailable {

            override val isCurrentFrontFacing: Boolean = false
        }
    }

    object NoCameras : CamerasInfo {

        override val isCurrentFrontFacing: Boolean? = null
    }

    object UnknownCameras : CamerasInfo {

        override val isCurrentFrontFacing: Boolean? = null
    }
}
