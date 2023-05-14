package org.landmarkscollector.domain.repository

import android.net.Uri
import org.landmarkscollector.domain.model.PathInfo
import kotlinx.coroutines.flow.Flow
import org.landmarkscollector.data.FrameLandmark
import org.landmarkscollector.data.Resource

interface ExportRepository {

    fun startExportData(
        exportList: List<FrameLandmark>,
        futureCsvFile: Uri
    ): Flow<Resource<PathInfo>>
}
