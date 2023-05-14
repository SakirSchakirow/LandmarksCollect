package org.landmarkscollector.domain.repository.csv

import com.opencsv.CSVWriterBuilder
import com.opencsv.ICSVWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.landmarkscollector.data.FrameLandmark
import org.landmarkscollector.data.Resource
import org.landmarkscollector.domain.model.GenerateInfo
import org.landmarkscollector.domain.repository.DataConverter
import java.io.StringWriter
import java.io.Writer

class DataConverterCSV : DataConverter {

    private fun getCsvWriter(writer: Writer): ICSVWriter {
        return CSVWriterBuilder(writer)
            .build()
    }

    override fun convertSensorData(
        exportDataList: List<FrameLandmark>
    ): Flow<Resource<GenerateInfo>> = flow {
        emit(Resource.Loading(GenerateInfo()))
        val writer = StringWriter()
        val csvWriter = getCsvWriter(writer)
        val valuesForOnePercent = (exportDataList.size / 100) + 1
        var alreadyConvertedValues = 0
        csvWriter.writeNext(HEADER_DATA)

        exportDataList.forEach { row ->
            csvWriter.writeNext(
                with(row) {
                    arrayOf(
                        "$frame",
                        "$landmarkIndex",
                        rowId,
                        type,
                        "${x ?: ""}",
                        "${y ?: ""}",
                        "${z ?: ""}"
                    )
                }
            )
            alreadyConvertedValues += 1
            if (alreadyConvertedValues % valuesForOnePercent == 0) {
                emit(
                    Resource.Loading(
                        GenerateInfo(
                            progressPercentage = alreadyConvertedValues / valuesForOnePercent
                        )
                    )
                )
            }
        }
        emit(
            Resource.Success(
                GenerateInfo(
                    byteArray = String(writer.buffer).toByteArray(),
                    progressPercentage = 100
                )
            )
        )
        csvWriter.close()
        writer.close()
    }

    companion object {
        val HEADER_DATA = arrayOf("frame", "landmark_index", "row_id", "type", "x", "y", "z")
    }
}
