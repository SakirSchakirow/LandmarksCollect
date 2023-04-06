package org.landmarkscollector.domain.repository

import kotlinx.coroutines.flow.Flow
import org.landmarkscollector.data.CsvRow
import org.landmarkscollector.data.Resource
import org.landmarkscollector.domain.model.GenerateInfo

interface DataConverter {

    fun convertSensorData(
        exportDataList: List<CsvRow>
    ): Flow<Resource<GenerateInfo>>
}
