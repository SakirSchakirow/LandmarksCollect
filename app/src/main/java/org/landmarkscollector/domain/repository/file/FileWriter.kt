package org.landmarkscollector.domain.repository.file

import android.net.Uri
import org.landmarkscollector.data.Resource

interface FileWriter {

    suspend fun writeFile(byteArray: ByteArray, futureCsvFile: Uri): Resource<Uri>
}
