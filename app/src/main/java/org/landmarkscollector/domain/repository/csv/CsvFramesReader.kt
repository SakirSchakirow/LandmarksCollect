package org.landmarkscollector.domain.repository.csv

import com.opencsv.CSVParser
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import org.landmarkscollector.data.FrameLandmark
import org.landmarkscollector.data.Frame
import org.landmarkscollector.data.Landmark
import org.landmarkscollector.domain.repository.FramesReader
import java.io.InputStream
import java.io.Reader

class CsvFramesReader : FramesReader {

    private fun getCsvReader(reader: Reader): CSVReader {
        return CSVReaderBuilder(reader)
            .withCSVParser(CSVParser())
            .build()
    }

    override fun readFrames(inputStream: InputStream): List<Frame> {
        val reader = inputStream.bufferedReader()
        val csvReader = getCsvReader(reader)
        // reading the header
        csvReader.readNext()
        return csvReader.asSequence()
            .map { row ->
                val (frame, landmarkIndex, rowId, type, x, y, z) = row
                FrameLandmark(
                    frame = frame.toUInt(),
                    landmarkIndex = landmarkIndex.toUInt(),
                    rowId = rowId,
                    type = type,
                    x = x.toFloatOrNull(),
                    y = y.toFloatOrNull(),
                    z = z.toFloatOrNull()
                )
            }
            .groupBy(FrameLandmark::frame)
            .map { (frameNumber, rows) ->
                Frame(frameNumber, rows.map(::toLandmark))
            }
    }

    private fun toLandmark(row: FrameLandmark): Landmark? = with(row) {
        return if (x != null && y != null && z != null) {
            when (type) {
                Landmark.LandmarkType.RightHand.label -> Landmark.Hand.Right(landmarkIndex, x, y, z)
                Landmark.LandmarkType.LeftHand.label -> Landmark.Hand.Left(landmarkIndex, x, y, z)
                Landmark.LandmarkType.Face.label -> Landmark.Face(landmarkIndex, x, y, z)
                Landmark.LandmarkType.Pose.label -> Landmark.Pose(landmarkIndex, x, y, z)
                else -> error("Unknown landmark type:$type")
            }
        } else {
            null
        }
    }

    private operator fun <String> Array<String>.component6(): String = this[5]

    private operator fun <String> Array<String>.component7(): String = this[6]
}
