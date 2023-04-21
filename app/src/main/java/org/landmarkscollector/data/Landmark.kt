package org.landmarkscollector.data

sealed class Landmark {
    abstract val type: LandmarkType
    abstract val landmarkIndex: Int
    abstract val x: Float
    abstract val y: Float
    abstract val z: Float

    sealed class Hand(
        override val landmarkIndex: Int,
        override val x: Float,
        override val y: Float,
        override val z: Float
    ) : Landmark() {

        class Left(
            override val landmarkIndex: Int,
            override val x: Float,
            override val y: Float,
            override val z: Float
        ) : Hand(landmarkIndex, x, y, z) {

            override val type = LandmarkType.LeftHand
        }

        class Right(
            override val landmarkIndex: Int,
            override val x: Float,
            override val y: Float,
            override val z: Float
        ) : Hand(landmarkIndex, x, y, z) {

            override val type = LandmarkType.RightHand
        }
    }

    class Face(
        override val landmarkIndex: Int,
        override val x: Float,
        override val y: Float,
        override val z: Float
    ) : Landmark() {

        override val type = LandmarkType.Face
    }

    class Pose(
        override val landmarkIndex: Int,
        override val x: Float,
        override val y: Float,
        override val z: Float
    ) : Landmark() {

        override val type = LandmarkType.Pose
    }

    enum class LandmarkType(val typeName: String, val totalLandmarkNumber: Int) {
        Face("face", 468),
        RightHand("right_hand", 21),
        LeftHand("left_hand", 21),
        Pose("pose", 33),
    }
}
