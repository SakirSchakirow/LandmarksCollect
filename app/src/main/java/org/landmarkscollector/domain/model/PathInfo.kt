package org.landmarkscollector.domain.model

import android.net.Uri

data class PathInfo(
    val uri: Uri? = null,
    val progressPercentage: Int = 0
)
