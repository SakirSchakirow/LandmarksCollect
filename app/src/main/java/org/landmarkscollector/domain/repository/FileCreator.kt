package org.landmarkscollector.domain.repository

import android.net.Uri

interface FileCreator {

    fun createFile(directoryUri: Uri, gestureName: String, gestureNum: UInt): Uri
}
