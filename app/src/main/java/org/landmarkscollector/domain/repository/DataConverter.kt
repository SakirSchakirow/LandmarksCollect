package org.landmarkscollector.domain.repository

import kotlinx.coroutines.flow.Flow
import org.landmarkscollector.data.FrameLandmark
import org.landmarkscollector.data.Resource
import org.landmarkscollector.domain.model.GenerateInfo

interface DataConverter {

    fun convertSensorData(
        exportDataList: List<FrameLandmark>
    ): Flow<Resource<GenerateInfo>>
}
