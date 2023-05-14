package org.landmarkscollector.domain.repository

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.landmarkscollector.data.FrameLandmark
import org.landmarkscollector.data.Resource
import org.landmarkscollector.domain.model.PathInfo
import org.landmarkscollector.domain.repository.file.FileWriter

class ExportRepositoryImpl(
    private val fileWriter: FileWriter,
    private val dataConverter: DataConverter
) : ExportRepository {
    override fun startExportData(
        exportList: List<FrameLandmark>,
        futureCsvFile: Uri
    ): Flow<Resource<PathInfo>> =
        dataConverter.convertSensorData(exportList).map { generateInfo ->
            when (generateInfo) {
                is Resource.Success -> {
                    generateInfo.data.byteArray?.let {
                        when (val result = fileWriter.writeFile(it, futureCsvFile)) {
                            is Resource.Success -> {
                                return@map Resource.Success(
                                    PathInfo(
                                        uri = result.data,
                                        progressPercentage = 100
                                    )
                                )
                            }

                            is Resource.Loading -> {
                                return@map Resource.Error(errorMessage = "Unknown Error")
                            }

                            is Resource.Error -> {
                                return@map Resource.Error(errorMessage = result.errorMessage)
                            }
                        }
                    } ?: return@map Resource.Error(errorMessage = "Unkonwn error occured")
                }

                is Resource.Error -> {
                    return@map Resource.Error(errorMessage = generateInfo.errorMessage)
                }

                is Resource.Loading -> {
                    return@map Resource.Loading(
                        PathInfo(
                            progressPercentage = generateInfo.data?.progressPercentage ?: 0
                        )
                    )
                }
            }
        }.flowOn(Dispatchers.IO)
}
