package org.landmarkscollector.domain.repository

import android.net.Uri
import org.landmarkscollector.domain.model.PathInfo
import kotlinx.coroutines.flow.Flow
import org.landmarkscollector.data.CsvRow
import org.landmarkscollector.data.Resource

interface ExportRepository {

    fun startExportData(
        exportList: List<CsvRow>,
        futureCsvFile: Uri
    ): Flow<Resource<PathInfo>>
}
