package org.landmarkscollector.domain.repository.csv

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.landmarkscollector.domain.repository.FileCreator

private const val CSV_MIME_TYPE = "text/csv"

class CsvFileCreator(val context: Context) : FileCreator {

    override fun createFile(directoryUri: Uri, gestureName: String, gestureNum: UInt): Uri {
        return DocumentFile.fromTreeUri(context, directoryUri)!!
            .createFile(
                CSV_MIME_TYPE,
                "${gestureName}_$gestureNum}"
            )!!.uri
    }
}
