package org.landmarkscollector.data

sealed class Landmark {
    abstract val type: LandmarkType
    abstract val landmarkIndex: UInt
    abstract val x: Float
    abstract val y: Float
    abstract val z: Float

    sealed class Hand(
        override val landmarkIndex: UInt,
        override val x: Float,
        override val y: Float,
        override val z: Float
    ) : Landmark() {

        class Left(
            override val landmarkIndex: UInt,
            override val x: Float,
            override val y: Float,
            override val z: Float
        ) : Hand(landmarkIndex, x, y, z) {

            override val type = LandmarkType.LeftHand
        }

        class Right(
            override val landmarkIndex: UInt,
            override val x: Float,
            override val y: Float,
            override val z: Float
        ) : Hand(landmarkIndex, x, y, z) {

            override val type = LandmarkType.RightHand
        }
    }

    class Face(
        override val landmarkIndex: UInt,
        override val x: Float,
        override val y: Float,
        override val z: Float
    ) : Landmark() {

        override val type = LandmarkType.Face
    }

    class Pose(
        override val landmarkIndex: UInt,
        override val x: Float,
        override val y: Float,
        override val z: Float
    ) : Landmark() {

        override val type = LandmarkType.Pose
    }

    enum class LandmarkType(val label: String, val totalLandmarkNumber: UInt) {
        Face("face", 468u),
        RightHand("right_hand", 21u),
        LeftHand("left_hand", 21u),
        Pose("pose", 33u)
    }
}
