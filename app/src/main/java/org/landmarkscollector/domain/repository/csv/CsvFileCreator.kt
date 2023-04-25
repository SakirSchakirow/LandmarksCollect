package org.landmarkscollector.domain.repository.csv

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.landmarkscollector.domain.repository.FileCreator


class CsvFileCreator(val context: Context) : FileCreator {

    override fun createFile(directoryUri: Uri, gestureName: String, gestureNum: UInt): Uri {
        return DocumentFile.fromTreeUri(context, directoryUri)!!
            .createFile(
                CSV_MIME_TYPE,
                getFileName(gestureName, gestureNum)
            )!!.uri
    }

    private fun getFileName(gestureName: String, gestureNum: UInt): String {
        return "${gestureName.noWhitespace()}_$gestureNum"
    }

    private fun String.noWhitespace(): String {
        return trim()
            .replace(WHITESPACE_REGEX, WHITESPACE_REPLACER)
    }

    companion object {

        private const val CSV_MIME_TYPE = "text/csv"
        private const val WHITESPACE_REPLACER = "_"
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
