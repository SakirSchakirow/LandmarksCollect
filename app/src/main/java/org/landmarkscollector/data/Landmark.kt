package org.landmarkscollector.data

sealed class Landmark {
    abstract val landmarkType: String
    abstract val landmarkNumber: Int
    abstract val x: Float
    abstract val y: Float
    abstract val z: Float

    sealed class Hand(
        override val landmarkNumber: Int,
        override val x: Float,
        override val y: Float,
        override val z: Float,
        handedness: String,
    ) : Landmark() {

        override val landmarkType: String = "${handedness}_hand"

        class Left(
            override val landmarkNumber: Int,
            override val x: Float,
            override val y: Float,
            override val z: Float
        ) : Hand(landmarkNumber, x, y, z, "left")

        class Right(
            override val landmarkNumber: Int,
            override val x: Float,
            override val y: Float,
            override val z: Float
        ) : Hand(landmarkNumber, x, y, z, "right")
    }

    class Face(
        override val landmarkNumber: Int,
        override val x: Float,
        override val y: Float,
        override val z: Float
    ) : Landmark() {

        override val landmarkType: String = "face"
    }

    class Pose(
        override val landmarkNumber: Int,
        override val x: Float,
        override val y: Float,
        override val z: Float
    ) : Landmark() {

        override val landmarkType: String = "pose"
    }
}
